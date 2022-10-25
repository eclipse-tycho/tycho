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
package org.eclipse.tycho.packaging.osgiresolve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.resource.Resource;

public class OSGiResolver {

	private BundleContext systemContext;
	private Map<String, ConnectModule> moduleMap = new LinkedHashMap<>();
	private Equinox equinox;
	private Set<String> installedIds = new HashSet<>();

	public OSGiResolver(File directory) throws BundleException {
		ModuleConnector moduleConnector = new ModuleConnector() {

			@Override
			public Optional<BundleActivator> newBundleActivator() {
				return Optional.empty();
			}

			@Override
			public void initialize(File storage, Map<String, String> configuration) {

			}

			@Override
			public Optional<ConnectModule> connect(String location) throws BundleException {
				return Optional.ofNullable(moduleMap.get(location));
			}
		};
		equinox = new Equinox(Map.of(Constants.FRAMEWORK_STORAGE, directory.getAbsolutePath()), moduleConnector);
		equinox.init();
		systemContext = equinox.getBundleContext();
	}

	public Bundle install(File item) throws BundleException, IOException {
		TestModule module = getModule(item);
		if (module != null && !module.getName().equals("org.eclipse.osgi") && installedIds.add(module.getId())) {
			String path = module.getLocation().getAbsolutePath();
			moduleMap.put(path, module);
			return systemContext.installBundle(path);
		}
		return null;
	}

	public Map<Bundle, String> resolve() {
		HashMap<Bundle, String> resultMap = new HashMap<>();
		Bundle[] bundles = systemContext.getBundles();
		ModuleContainer container = systemContext.getBundle().adapt(Module.class).getContainer();
		List<Module> list = Arrays.stream(bundles).filter(b -> b != systemContext.getBundle())
				.map(b -> b.adapt(Module.class)).filter(Objects::nonNull).collect(Collectors.toList());
		ResolutionReport report = container.resolve(list, false);
		Map<Resource, List<org.eclipse.osgi.report.resolution.ResolutionReport.Entry>> entries = report.getEntries();
		for (Entry<Resource, List<org.eclipse.osgi.report.resolution.ResolutionReport.Entry>> module : entries
				.entrySet()) {
			Resource key = module.getKey();
			if (key instanceof BundleReference) {
				BundleReference reference = (BundleReference) key;
				Bundle bundle = reference.getBundle();
				if (bundle.getState() == Bundle.INSTALLED) {
					String message = report.getResolutionReportMessage(key);
					if (message != null && !message.isBlank()) {
						resultMap.put(bundle, message);
					}
				}
			}
		}
		return resultMap;
	}

	private static TestModule getModule(File file) throws IOException {
		Manifest manifest;
		if (file.isFile()) {
			if (file.length() == 0) {
				return null;
			}
			try (JarFile jarFile = new JarFile(file)) {
				manifest = jarFile.getManifest();
			}
		} else if (file.isDirectory()) {
			File manifestFile = new File(file, JarFile.MANIFEST_NAME);
			if (manifestFile.isFile()) {
				try (InputStream stream = new FileInputStream(manifestFile)) {
					manifest = new Manifest(stream);
				}
			} else {
				return null;
			}
		} else {
			throw new IOException("not a file or directory");
		}
		Attributes attributes = manifest.getMainAttributes();
		String value = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (value != null) {
			Map<String, String> headers = new LinkedHashMap<>();
			for (Entry<Object, Object> entry : attributes.entrySet()) {
				String key = entry.getKey().toString();
				if (Constants.BUNDLE_ACTIVATOR.equalsIgnoreCase(key)) {
					continue;
				}
				if (Constants.REQUIRE_CAPABILITY.equalsIgnoreCase(key)
						|| "Eclipse-PlatformFilter".equalsIgnoreCase(key)) {
					// capabilities are not really something that is interesting for maven...
					continue;
				}
				if ("Service-Component".equalsIgnoreCase(key)) {
					continue;
				}
				headers.put(key, entry.getValue().toString());
			}
			return new TestModule(attributes.getValue(Constants.BUNDLE_SYMBOLICNAME).split(";")[0],
					Version.parseVersion(attributes.getValue(Constants.BUNDLE_VERSION)), headers, file);
		}
		return null;
	}

}
