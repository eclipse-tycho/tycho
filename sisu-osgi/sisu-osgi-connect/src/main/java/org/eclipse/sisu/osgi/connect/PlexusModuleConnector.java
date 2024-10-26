/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.osgi.connect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.slf4j.Logger;

/**
 * The PlexusModuleConnector scans a linear classpath for bundles and install
 * them as {@link ConnectContent} into the given {@link BundleContext}
 */
final class PlexusModuleConnector implements ModuleConnector {

	private static final BundleInfo DEFAULT_BUNDLE_INFO = new BundleInfo(false, false);

	static final String MAVEN_EXTENSION_DESCRIPTOR = "META-INF/maven/extension.xml";

	private Map<String, PlexusConnectContent> modulesMap = new HashMap<>();

	private File storage;

	private Map<ClassRealm, List<String>> realmBundles = new HashMap<>();

	private URI frameworkBundle;

	private Set<String> installedSingletons = new HashSet<>();

	public PlexusModuleConnector(ConnectFrameworkFactory factory) {
		frameworkBundle = PlexusConnectFramework.getLocationFromClass(factory.getClass());
	}

	private String getBsn(String value) {
		if (value != null) {
			return value.split(";")[0].trim();
		}
		return null;
	}

	@Override
	public Optional<ConnectModule> connect(String location) throws BundleException {
		return Optional.ofNullable(modulesMap.get(location));
	}

	@Override
	public void initialize(File storage, Map<String, String> configuration) {
		this.storage = storage;
	}

	@Override
	public Optional<BundleActivator> newBundleActivator() {
		return Optional.empty();
	}

	public File getStorage() {
		return storage;
	}

	public synchronized void installRealm(ClassRealm realm, BundleContext bundleContext, Logger logger) {
		Objects.requireNonNull(realm);
		if (realmBundles.containsKey(realm)) {
			// already scanned!
			return;
		}
		logger.debug("Scan realm " + realm.getId());
		ClassRealm parentRealm = realm.getParentRealm();
		if (parentRealm != null) {
			// first install the parent realm...
			installRealm(parentRealm, bundleContext, logger);
		}
		// make the realm available as a bundle exporting any packages it provides...
		List<String> installed = new ArrayList<>();
		realmBundles.put(realm, installed);
		RealmExports realmExports = readCoreExports(logger, realm);
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("Manifest-Version", "1.0");
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		String realmBundleName = getRealmBundle(realm);
		headers.put(Constants.BUNDLE_SYMBOLICNAME, realmBundleName);
		headers.put(Constants.BUNDLE_VERSION, "1.0.0." + System.identityHashCode(realm));
		if (!realmExports.packages.isEmpty()) {
			headers.put(Constants.EXPORT_PACKAGE, String.join(",", realmExports.packages));
		}
		Collection<ClassRealm> importRealms = realm.getImportRealms();
		if (!importRealms.isEmpty()) {
			headers.put(Constants.REQUIRE_BUNDLE,
					importRealms.stream().map(PlexusModuleConnector::getRealmBundle).collect(Collectors.joining(",")));
		}
		modulesMap.put(realmBundleName, new PlexusConnectContent(null, headers, realm));
		logger.debug("Installing " + realmBundleName + " with headers " + headers.entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("\r\n")));
		if (installBundle(bundleContext, realmBundleName, logger) != null) {
			installed.add(realmBundleName);
		}
		boolean isExtensionRealm = !realmExports.artifacts.isEmpty() || !realmExports.bundleInfoMap.isEmpty();
		if (isExtensionRealm && realmExports.artifacts.isEmpty() && realmExports.bundleInfoMap.isEmpty()) {
			// nothing more to do...
			return;
		}
		// now scan the URLs
		for (URL url : realm.getURLs()) {
			File file = getFile(url);
			if (file == null) {
				logger.debug("Cannot convert URL " + url + " to File");
				continue;
			}
			// TODO we should actually make sure that no other libs drip in here, but need
			// to disable this until some version ranges are fixed for platform, so
			// hopefully on next release. Otherwhise there is a risk of pull in incompatible
			// versions.
//			if (!realmExports.jars.isEmpty() && !realmExports.jars.contains(file.getName())) {
//				logger.debug("Skip " + file + " as it is not part of the dependency jars...");
//				continue;
//			}
			try {
				JarFile jarFile = new JarFile(file);
				try {
					Attributes mainAttributes = getAttributes(jarFile);
					if (mainAttributes == null
							|| PlexusConnectFramework.locationsMatch(frameworkBundle, file.getAbsolutePath())) {
						jarFile.close();
						continue;
					}
					String bundleSymbolicName = getBsn(mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
					if (isExtensionRealm && !realmExports.bundleInfoMap.containsKey(bundleSymbolicName)) {
						String artifactKey = getArtifactKey(jarFile);
						if (artifactKey == null || !realmExports.artifacts.contains(artifactKey)) {
							String identifier = artifactKey == null ? file.getName() : artifactKey;
							if (bundleSymbolicName != null) {
								identifier += " (" + bundleSymbolicName + ")";
							}
							logger.debug("Skip " + identifier + " as it is not exported by the extension realm.");
							jarFile.close();
							continue;
						} else {
							logger.debug("Checking exported artifact " + artifactKey);
						}
					}
					if (bundleSymbolicName == null) {
						logger.debug("File " + file + " is not a bundle");
						jarFile.close();
						continue;
					}
					BundleInfo info = realmExports.bundleInfoMap.getOrDefault(bundleSymbolicName, DEFAULT_BUNDLE_INFO);
					String bundleVersion = mainAttributes.getValue(Constants.BUNDLE_VERSION);
					logger.debug("Discovered bundle " + bundleSymbolicName + " (" + bundleVersion + ") @ " + file);
					String location = file.getAbsolutePath();
					Bundle bundle;
					if (modulesMap.containsKey(location)) {
						bundle = bundleContext.getBundle(location);
					} else if (isSingleton(mainAttributes) && !installedSingletons.add(bundleSymbolicName)) {
						bundle = Arrays.stream(bundleContext.getBundles())
								.filter(b -> b.getSymbolicName().equals(bundleSymbolicName)).findFirst().orElse(null);
						logger.debug("More than one singleton bundle found for smybolic name " + bundleSymbolicName
								+ " one with path " + location + " and one with path "
								+ (bundle == null ? "???" : bundle.getLocation()));
					} else {
						modulesMap.put(location,
								new PlexusConnectContent(jarFile, getHeaderFromManifest(jarFile),
										info.isolated ? null : realm));
						bundle = installBundle(bundleContext, location, logger);
					}
					if (bundle != null) {
						installed.add(location);
						if (info.start) {
							try {
								bundle.start();
							} catch (BundleException e) {
							}
						}
					}
				} catch (IOException e) {
					logger.warn("Cannot process jar at " + file, e);
					jarFile.close();
				}
			} catch (IOException e) {
				logger.warn("Cannot open jar at " + file, e);
			}
		}
	}

	private static boolean isSingleton(Attributes mainAttributes) {
		String bsn = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
		return bsn != null && bsn.contains("singleton:=true");
	}

	private static String getRealmBundle(ClassRealm realm) {
		return "sisu.connect.realm." + realm.getId().replace('>', '.').replace(':', '.');
	}

	private static String getArtifactKey(JarFile jarFile) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			String name = jarEntry.getName();
			if (name.startsWith("META-INF/maven/") && name.endsWith("pom.properties")) {
				try (InputStream stream = jarFile.getInputStream(jarEntry)) {
					Properties properties = new Properties();
					properties.load(stream);
					return properties.getProperty("groupId") + ":" + properties.getProperty("artifactId");
				}
			}
		}
		return null;
	}

	protected RealmExports readCoreExports(Logger logger, ClassRealm classRealm) {
		RealmExports exports = new RealmExports();
		Enumeration<URL> resources = classRealm.loadResourcesFromSelf(MAVEN_EXTENSION_DESCRIPTOR);
		while (resources != null && resources.hasMoreElements()) {
			URL url = resources.nextElement();
			try (InputStream stream = url.openStream()) {
				parseCoreExports(stream, exports);
			} catch (IOException e) {
				logger.warn("Cannot process extension descriptor from " + url, e);
			}
		}
		exports.bundleInfoMap.putAll(readBundles(classRealm, logger));
		exports.jars.addAll(readJars(classRealm, logger));
		return exports;
	}

	protected Bundle installBundle(BundleContext bundleContext, String location, Logger logger) {
		try {
			Bundle bundle = bundleContext.installBundle(location);
			return bundle;
		} catch (BundleException e) {
			if (logger.isDebugEnabled()) {
				logger.warn("Cannot install bundle at " + location, e);
			} else {
				logger.warn("Cannot install bundle at " + location + ": " + e.getMessage());
			}
			PlexusConnectContent content = modulesMap.remove(location);
			if (content != null) {
				try {
					content.close();
				} catch (IOException e1) {
				}
			}
		}
		return null;
	}

	private void parseCoreExports(InputStream stream, RealmExports exports) throws IOException {
		try {
			Xpp3Dom dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(stream));
			if (!"extension".equals(dom.getName())) {
				return;
			}
			Xpp3Dom exportedPackages = dom.getChild("exportedPackages");
			if (exportedPackages != null) {
				Xpp3Dom[] children = exportedPackages.getChildren("exportedPackage");
				for (Xpp3Dom child : children) {
					String value = child.getValue();
					if (value.endsWith(".*")) {
						value = value.substring(0, value.length() - 2);
					}
					exports.packages.add(value);
				}
			}
			Xpp3Dom exportedArtifacts = dom.getChild("exportedArtifacts");
			if (exportedArtifacts != null) {
				Xpp3Dom[] children = exportedArtifacts.getChildren("exportedArtifact");
				for (Xpp3Dom child : children) {
					String value = child.getValue();
					if (value.endsWith(".*")) {
						value = value.substring(0, value.length() - 2);
					}
					exports.artifacts.add(value);
				}
			}
		} catch (XmlPullParserException e) {
			throw new IOException("parsing failed!", e);
		}

	}

	private Map<String, String> getHeaderFromManifest(JarFile jarFile) throws IOException {
		Attributes attributes = jarFile.getManifest().getMainAttributes();
		Map<String, String> headers = new LinkedHashMap<>();
		attributes.forEach((key, value) -> headers.put(key.toString(), value.toString()));
		return Collections.unmodifiableMap(headers);
	}

	public synchronized void disposeRealm(ClassRealm realm, BundleContext bundleContext, Logger logger) {
		disposeChilds(realm, bundleContext, logger);
		List<String> remove = realmBundles.remove(realm);
		if (remove != null && !remove.isEmpty()) {
			logger.debug("Removing realm " + realm.getId() + " uninstalls " + remove.size() + " bundle(s)");
			for (String location : remove) {
				Bundle bundle = bundleContext.getBundle(location);
				if (bundle != null) {
					try {
						bundle.uninstall();
					} catch (BundleException e) {
						if (logger.isDebugEnabled()) {
							logger.warn("Cannot uninstall bundle " + bundle.getSymbolicName(), e);
						} else {
							logger.warn("Cannot uninstall bundle " + bundle.getSymbolicName() + ": " + e);
						}
					}
				}
			}
		}
	}

	private void disposeChilds(ClassRealm realm, BundleContext bundleContext, Logger logger) {
		for (ClassRealm child : realmBundles.keySet().toArray(ClassRealm[]::new)) {
			if (child.getParentRealm() == realm) {
				disposeRealm(child, bundleContext, logger);
			}
		}

	}

	private Attributes getAttributes(JarFile jarFile) throws IOException {
		Manifest manifest = jarFile.getManifest();
		if (manifest == null) {
			return null;
		}
		return manifest.getMainAttributes();
	}

	private static File getFile(URL url) {
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			try {
				File file = new File(url.toURI());
				if (file.getName().toLowerCase().endsWith(".jar") && file.isFile()) {
					return file;
				}
			} catch (URISyntaxException e) {
			}
		}
		return null;
	}

	private static final class RealmExports {
		final Set<String> packages = new HashSet<>();
		final Set<String> artifacts = new HashSet<>();
		final Set<String> jars = new HashSet<>();
		final Map<String, BundleInfo> bundleInfoMap = new LinkedHashMap<>();
	}

	private static Set<String> readJars(ClassRealm realm, Logger logger) {
		Set<String> jars = new HashSet<>();
		Enumeration<URL> resources = realm.loadResourcesFromSelf("META-INF/sisu/connect.dependencies");
		while (resources != null && resources.hasMoreElements()) {
			URL url = resources.nextElement();
			logger.debug("Reading jars from " + url);
			Properties properties = new Properties();
			try (InputStream stream = url.openStream()) {
				properties.load(stream);
				for (String key : properties.stringPropertyNames()) {
					String[] split = key.split("/", 3);
					if (split.length == 3) {
						if ("version".equals(split[2])) {
							String version = properties.getProperty(key);
							String artifactId = split[1];
							jars.add(artifactId + "-" + version
									+ ".jar");
						}
					}
				}
			} catch (IOException e) {
				logger.warn("Cannot read jar infos from url " + url);
			}
		}
		return jars;
	}

	private static Map<String, BundleInfo> readBundles(ClassRealm realm, Logger logger) {
		Map<String, BundleInfo> bundleInfos = new LinkedHashMap<>();
		Enumeration<URL> resources = realm.loadResourcesFromSelf("META-INF/sisu/connect.bundles");
		while (resources != null && resources.hasMoreElements()) {
			URL url = resources.nextElement();
			logger.debug("Reading extra bundles from " + url);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				reader.lines().forEachOrdered(line -> {
					if (line.startsWith("#") || line.isBlank()) {
						return;
					}
					String[] split = line.split(",", 2);
					boolean start;
					if (split.length == 2) {
						start = Boolean.parseBoolean(split[1]);
					} else {
						start = false;
					}
					String bsn = split[0];
					boolean isolated = bsn.startsWith(">");
					if (isolated) {
						bsn = bsn.substring(1);
					}
					bundleInfos.put(bsn, new BundleInfo(start, isolated));
				});
			} catch (IOException e) {
				logger.warn("Cannot read bundle infos from url " + url);
			}
		}
		return bundleInfos;
	}

	private static final record BundleInfo(boolean start, boolean isolated) {

	}

}
