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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.codehaus.plexus.logging.Logger;
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

/**
 * The PlexusModuleConnector scans a linear classpath for bundles and install
 * them as {@link ConnectContent} into the given {@link BundleContext}
 */
final class PlexusModuleConnector implements ModuleConnector {

	static final String MAVEN_EXTENSION_DESCRIPTOR = "META-INF/maven/extension.xml";

	private Map<String, PlexusConnectContent> modulesMap = new HashMap<>();

	private File storage;

	private Map<ClassRealm, List<String>> realmBundles = new HashMap<>();

	private String frameworkBundle;

	public PlexusModuleConnector(ConnectFrameworkFactory factory) {
		frameworkBundle = PlexusFrameworkUtilHelper.getLocationFromClass(factory.getClass());
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
		List<String> installed = new ArrayList<String>();
		realmBundles.put(realm, installed);
		RealmExports realmExports = readCoreExports(logger, realm);
		LinkedHashMap<String, String> headers = new LinkedHashMap<>();
		headers.put("Manifest-Version", "1.0");
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		String realmBundleName = getRealmBundle(realm);
		headers.put(Constants.BUNDLE_SYMBOLICNAME, realmBundleName);
		headers.put(Constants.BUNDLE_VERSION, "1.0.0." + System.identityHashCode(realm));
		if (!realmExports.packages.isEmpty()) {
			headers.put(Constants.EXPORT_PACKAGE, realmExports.packages.stream().collect(Collectors.joining(",")));
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
		boolean isExtensionRealm = !realmExports.artifacts.isEmpty() || !realmExports.artifacts.isEmpty()
				|| !realmExports.bundleStartMap.isEmpty();
		if (isExtensionRealm && realmExports.artifacts.isEmpty() && realmExports.bundleStartMap.isEmpty()) {
			// nothing more to do...
			return;
		}
		// now scan the URLs
		for (URL url : realm.getURLs()) {
			File file = getFile(logger, url);
			if (file == null) {
				logger.warn("Can't open convert url " + url + " to file!");
				continue;
			}
			try {
				JarFile jarFile = new JarFile(file);
				try {
					Attributes mainAttributes = getAttributes(jarFile);
					if (mainAttributes == null
							|| PlexusFrameworkUtilHelper.locationsMatch(frameworkBundle, file.getAbsolutePath())) {
						jarFile.close();
						continue;
					}
					String bundleSymbolicName = getBsn(mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
					if (isExtensionRealm && !realmExports.bundleStartMap.containsKey(bundleSymbolicName)) {
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
							logger.debug("Checking exported artifact " + artifactKey + "...");
						}
					}
					if (bundleSymbolicName == null) {
						logger.debug("File " + file + " is not a bundle...");
						jarFile.close();
						continue;
					}
					logger.debug("Discovered bundle " + bundleSymbolicName + " @ " + file);
					String location = file.getAbsolutePath();
					modulesMap.put(location, new PlexusConnectContent(jarFile, getHeaderFromManifest(jarFile), realm));
					Bundle bundle = installBundle(bundleContext, location, logger);
					if (bundle != null) {
						installed.add(location);
						if (realmExports.bundleStartMap.getOrDefault(bundleSymbolicName, false)) {
							try {
								bundle.start();
							} catch (BundleException e) {
							}
						}
					}
				} catch (IOException e) {
					logger.warn("Can't process jar at " + file, e);
					jarFile.close();
				}
			} catch (IOException e) {
				logger.warn("Can't open jar at " + file, e);
			}
		}
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
				logger.warn("Can't process extension descriptor from " + url, e);
			}
		}
		readBundles(classRealm, exports.bundleStartMap, logger);
		return exports;
	}

	protected Bundle installBundle(BundleContext bundleContext, String location, Logger logger) {
		try {
			Bundle bundle = bundleContext.installBundle(location);
			String policy = bundle.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY);
			if (Constants.ACTIVATION_LAZY.equals(policy)) {
				try {
					bundle.start();
				} catch (BundleException e) {
				}
			}
			return bundle;
		} catch (BundleException e) {
			if (logger.isDebugEnabled()) {
				logger.warn("Can't install bundle at " + location, e);
			} else {
				logger.warn("Can't install bundle at " + location + ": " + e.getMessage());
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
		Map<String, String> headers = new LinkedHashMap<String, String>();
		for (Entry<Object, Object> entry : attributes.entrySet()) {
			headers.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return Collections.unmodifiableMap(headers);
	}

	public synchronized void disposeRealm(ClassRealm realm, BundleContext bundleContext, Logger logger) {
		disposeChilds(realm, bundleContext, logger);
		List<String> remove = realmBundles.remove(realm);
		if (remove != null && !remove.isEmpty()) {
			logger.debug("Remove realm " + realm.getId() + " uninstall " + remove.size() + " bundle(s)...");
			for (String location : remove) {
				Bundle bundle = bundleContext.getBundle(location);
				if (bundle != null) {
					try {
						bundle.uninstall();
					} catch (BundleException e) {
						if (logger.isDebugEnabled()) {
							logger.warn("Can't uninstall bundle " + bundle.getSymbolicName(), e);
						} else {
							logger.warn("Can't uninstall bundle " + bundle.getSymbolicName() + ": " + e);
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
		Attributes mainAttributes = manifest.getMainAttributes();
		return mainAttributes;
	}

	private static File getFile(Logger logger, URL url) {
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
		final Set<String> packages = new HashSet<String>();
		final Set<String> artifacts = new HashSet<String>();
		final Map<String, Boolean> bundleStartMap = new LinkedHashMap<String, Boolean>();
	}

	private static void readBundles(ClassRealm realm, Map<String, Boolean> map, Logger logger) {
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
					map.put(split[0], start);
				});
			} catch (IOException e) {
				logger.warn("Can't read bundle infos from url " + url);
			}
		}
	}

}