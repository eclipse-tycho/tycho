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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Set;
import java.util.jar.Attributes;
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

	private static final String MAVEN_EXTENSION_DESCRIPTOR = "META-INF/maven/extension.xml";

	private ClassLoader classloader;

	private Map<String, PlexusConnectContent> modulesMap = new HashMap<>();

	private File storage;

	private Map<ClassRealm, List<String>> realmBundles = new HashMap<>();

	private String frameworkBundle;

	public PlexusModuleConnector(ClassLoader classloader, ConnectFrameworkFactory factory) {
		this.classloader = classloader;
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

	public synchronized void scanRealm(ClassRealm realm, BundleContext bundleContext, Logger logger) {
		Objects.requireNonNull(realm);
		if (realmBundles.containsKey(realm)) {
			// already scanned!
			return;
		}
		logger.debug("Scan realm " + realm.getId());
		ClassRealm parentRealm = realm.getParentRealm();
		if (parentRealm != null) {
			scanRealm(parentRealm, bundleContext, logger);
		}
		URL[] urLs = realm.getURLs();
		List<String> installed = new ArrayList<String>();
		realmBundles.put(realm, installed);
		for (URL url : urLs) {
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
					if (bundleSymbolicName == null) {
						logger.debug("File " + file + " is not a bundle...");
						jarFile.close();
						continue;
					}
					logger.debug("Discovered bundle " + bundleSymbolicName + " @ " + file);
					String location = file.getAbsolutePath();
					modulesMap.put(location,
							new PlexusConnectContent(jarFile, getHeaderFromManifest(jarFile), classloader));
					if (installBundle(bundleContext, location, logger) != null) {
						installed.add(location);
					}
				} catch (IOException e) {
					logger.warn("Can't process jar at " + file, e);
					jarFile.close();
				}
			} catch (IOException e) {
				logger.warn("Can't open jar at " + file, e);
			}
		}
		Collection<ClassRealm> importRealms = realm.getImportRealms();
		// TODO it would be better if plexus would expose the packages from the
		// classrealm!
		for (ClassRealm classRealm : importRealms) {
			Set<String> coreExports = new HashSet<String>();
			try {
				Enumeration<URL> resources = classRealm.getResources(MAVEN_EXTENSION_DESCRIPTOR);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					try (InputStream stream = url.openStream()) {
						parseCoreExports(stream, coreExports);
					} catch (IOException e) {
						logger.warn("Can't process extension descriptor from " + url, e);
					}
				}
			} catch (IOException e) {
				logger.warn("Can't process extension descriptors", e);
			}
			if (!coreExports.isEmpty()) {
				LinkedHashMap<String, String> headers = new LinkedHashMap<>();
				headers.put("Manifest-Version", "1.0");
				headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
				String bsn = "plexus.realm." + classRealm.getId();
				headers.put(Constants.BUNDLE_SYMBOLICNAME, bsn);
				headers.put(Constants.BUNDLE_VERSION, "1.0.0." + System.identityHashCode(classRealm));
				headers.put(Constants.EXPORT_PACKAGE, coreExports.stream().collect(Collectors.joining(",")));
				modulesMap.put(bsn, new PlexusConnectContent(null, headers, classloader));
				logger.debug("Installing " + bsn + " exporting core packages " + coreExports);
				if (installBundle(bundleContext, bsn, logger) != null) {
					installed.add(bsn);
				}
			}
		}
	}

	protected Bundle installBundle(BundleContext bundleContext, String location, Logger logger) {
		try {
			return bundleContext.installBundle(location);
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

	private void parseCoreExports(InputStream stream, Set<String> coreExports) throws IOException {
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
					coreExports.add(value);
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

}