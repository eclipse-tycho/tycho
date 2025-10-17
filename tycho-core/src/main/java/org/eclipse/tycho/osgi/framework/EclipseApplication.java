/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.osgi.framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IllegalArtifactReferenceException;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.TargetPlatformConfigurationException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * represents an eclipse application that can be launched
 */
public class EclipseApplication {

    public static final String ARG_APPLICATION = "-application";

    private static final Set<String> ALWAYS_START_BUNDLES = Set.of(Bundles.BUNDLE_CORE, Bundles.BUNDLE_SCR,
            Bundles.BUNDLE_APP);
    private P2Resolver resolver;
    private TargetPlatform targetPlatform;
    private Logger logger;
    private boolean needResolve;
    private List<Path> resolvedBundles;
    private String name;
    private Map<String, String> frameworkProperties = new LinkedHashMap<>();
    private Predicate<LogEntry> loggingFilter = always -> true;
    private Set<String> startBundles = new HashSet<>(ALWAYS_START_BUNDLES);
    private Map<File, MavenProject> baseDirMap;

    EclipseApplication(String name, P2Resolver resolver, TargetPlatform targetPlatform, Logger logger,
            Map<File, MavenProject> baseDirMap) {
        this.name = name;
        this.resolver = resolver;
        this.targetPlatform = targetPlatform;
        this.logger = logger;
        this.baseDirMap = baseDirMap;
    }

    public synchronized Collection<Path> getApplicationBundles() {
        if (needResolve) {
            resolvedBundles = resolveBundles(resolver);
            if (logger.isDebugEnabled()) {
                logger.debug("Eclipse Application " + name + " resolved with " + resolvedBundles.size() + " bundles.");
                for (Path path : resolvedBundles) {
                    logger.debug("\t" + path);
                }
            }
        }
        return resolvedBundles;
    }

    private List<Path> resolveBundles(P2Resolver resolver) {
        List<Path> resolvedBundles = new ArrayList<>();

        for (P2ResolutionResult result : resolver.resolveTargetDependencies(targetPlatform, null).values()) {
            for (Entry entry : result.getArtifacts()) {
                if (ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(entry.getType())
                        && !"org.eclipse.osgi".equals(entry.getId())) {
                    File location = entry.getLocation(true);
                    if (location == null) {
                        logger.warn("Cannot resolve location for bundle " + entry.getId() + " " + entry.getVersion()
                                + ". The artifact may not be available or failed to download.");
                        continue;
                    }
                    Path path = location.toPath();
                    if (location.isDirectory()) {
                        MavenProject mavenProject = baseDirMap.get(location);
                        if (mavenProject != null) {
                            File artifactFile = mavenProject.getArtifact().getFile();
                            if (artifactFile != null && artifactFile.exists()) {
                                path = artifactFile.toPath();
                            }
                        }
                    }
                    resolvedBundles.add(path);
                }
            }
        }
        return resolvedBundles;
    }

    /**
     * Add a bundle to the application
     * 
     * @param bundleSymbolicName
     */
    public synchronized void addBundle(String bundleSymbolicName) {
        try {
            resolver.addDependency(ArtifactType.TYPE_ECLIPSE_PLUGIN, bundleSymbolicName, "0.0.0");
            needResolve = true;
        } catch (IllegalArtifactReferenceException e) {
            throw new TargetPlatformConfigurationException("Can't add API tools requirement", e);
        }
    }

    /**
     * Add a feature to the application
     * 
     * @param featureId
     */
    public synchronized void addFeature(String featureId) {
        try {
            resolver.addDependency(ArtifactType.TYPE_ECLIPSE_FEATURE, featureId, "0.0.0");
            needResolve = true;
        } catch (IllegalArtifactReferenceException e) {
            throw new TargetPlatformConfigurationException("Can't add API tools requirement", e);
        }
    }

    /**
     * Adds a product IU
     * 
     * @param bundleSymbolicName
     */
    public synchronized void addProduct(String productId) {
        try {
            resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, productId, "0.0.0");
            needResolve = true;
        } catch (IllegalArtifactReferenceException e) {
            throw new TargetPlatformConfigurationException("Can't add API tools requirement", e);
        }
    }

    /**
     * Adds a conditional bundle
     * 
     * @param bundleSymbolicName
     * @param filter
     */
    public synchronized void addConditionalBundle(String bundleSymbolicName, String filter) {
        IRequirement requirement = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID,
                bundleSymbolicName, VersionRange.emptyRange, filter, true, false, true);
        resolver.addRequirement(requirement);
        needResolve = true;
    }

    /**
     * @return the name of the application (might be used for logging
     */
    public String getName() {
        return name;
    }

    /**
     * Add a framework property
     * 
     * @param key
     * @param value
     */
    public void addFrameworkProperty(String key, String value) {
        frameworkProperties.put(key, value);
    }

    public void setLoggingFilter(Predicate<LogEntry> loggingFilter) {
        this.loggingFilter = loggingFilter;
    }

    public void setBundleStart(String id, boolean start) {
        if (start) {
            startBundles.add(id);
        } else {
            startBundles.remove(id);
        }
    }

    public <T> EclipseFramework startFramework(EclipseWorkspace<T> workspace, List<String> applicationArguments)
            throws BundleException {
        Map<String, String> frameworkProperties = getFrameworkProperties(workspace.getWorkDir());
        frameworkProperties.putAll(this.frameworkProperties);
        if (!applicationArguments.contains(ARG_APPLICATION)) {
            frameworkProperties.put("eclipse.ignoreApp", "true");
            frameworkProperties.put("osgi.noShutdown", "true");
        }
        ServiceLoader<ConnectFrameworkFactory> loader = ServiceLoader.load(ConnectFrameworkFactory.class,
                getClass().getClassLoader());
        ConnectFrameworkFactory factory = loader.findFirst()
                .orElseThrow(() -> new BundleException("No ConnectFrameworkFactory found"));
        EclipseModuleConnector connector = new EclipseModuleConnector();
        Framework framework = factory.newFramework(frameworkProperties, connector);
        framework.init();
        BundleContext systemBundleContext = framework.getBundleContext();
        EquinoxConfiguration configuration = setupArguments(systemBundleContext, applicationArguments);
        setupLogging(systemBundleContext);
        for (Path bundleFile : getApplicationBundles()) {
            String location = bundleFile.toUri().toString();
            Bundle bundle = systemBundleContext.getBundle(location);
            if (bundle == null) {
                String swt = connector.loadSWT(bundleFile);
                if (swt != null) {
                    bundle = systemBundleContext.installBundle(swt);
                } else {
                    //not installed yet...
                    if (Files.isDirectory(bundleFile)) {
                        bundle = systemBundleContext.installBundle(location);
                    } else if (isDirectoryBundle(bundleFile)) {
                        Path explodePath = workspace.getWorkDir().resolve("exploded").resolve(bundleFile.getFileName());
                        try {
                            Files.createDirectories(explodePath);
                            ZipUnArchiver unArchiver = new ZipUnArchiver(bundleFile.toFile());
                            unArchiver.setDestDirectory(explodePath.toFile());
                            unArchiver.extract();
                        } catch (IOException e) {
                            throw new BundleException("can't explode bundle " + bundleFile, e);
                        }
                        bundle = systemBundleContext.installBundle(explodePath.toUri().toASCIIString());
                    } else {
                        try (InputStream stream = Files.newInputStream(bundleFile)) {
                            bundle = systemBundleContext.installBundle(location, stream);
                        } catch (IOException e) {
                            throw new BundleException("can't read bundle " + bundleFile, e);
                        }
                    }
                }
            }
            if (startBundles.contains(bundle.getSymbolicName())) {
                bundle.start();
            }
        }
        FrameworkWiring wiring = framework.adapt(FrameworkWiring.class);
        wiring.resolveBundles(Collections.emptyList());
        return new EclipseFramework(framework, configuration, this, connector);
    }

    private boolean isDirectoryBundle(Path bundleFile) {
        try (JarFile jarFile = new JarFile(bundleFile.toFile())) {
            return "dir".equals(jarFile.getManifest().getMainAttributes().getValue("Eclipse-BundleShape"));
        } catch (IOException e1) {
        }
        return false;
    }

    private void setupLogging(BundleContext bundleContext) {
        LogListener logListener = entry -> {
            if (!loggingFilter.test(entry) && !logger.isDebugEnabled()) {
                return;
            }
            switch (entry.getLogLevel()) {
            case AUDIT:
            case ERROR:
                logger.error(entry.getMessage(), entry.getException());
                break;
            case WARN:
                logger.warn(entry.getMessage(), entry.getException());
                break;
            case INFO:
                logger.info(entry.getMessage(), entry.getException());
                break;
            case TRACE:
            case DEBUG:
                logger.debug(entry.getMessage(), entry.getException());
                break;
            }
        };
        ServiceTracker<LogReaderService, LogReaderService> serviceTracker = new ServiceTracker<>(bundleContext,
                LogReaderService.class, new ServiceTrackerCustomizer<>() {

                    @Override
                    public LogReaderService addingService(ServiceReference<LogReaderService> reference) {
                        LogReaderService service = bundleContext.getService(reference);
                        if (service != null) {
                            service.addLogListener(logListener);
                        }
                        return service;
                    }

                    @Override
                    public void modifiedService(ServiceReference<LogReaderService> reference,
                            LogReaderService service) {
                    }

                    @Override
                    public void removedService(ServiceReference<LogReaderService> reference, LogReaderService service) {
                        service.removeLogListener(logListener);
                        bundleContext.ungetService(reference);
                    }
                });
        serviceTracker.open();

    }

    private EquinoxConfiguration setupArguments(BundleContext systemBundleContext, List<String> applicationArguments) {
        ServiceTracker<EnvironmentInfo, EnvironmentInfo> environmentInfoTracker = new ServiceTracker<>(
                systemBundleContext, EnvironmentInfo.class, null);
        environmentInfoTracker.open();
        EquinoxConfiguration configuration = (EquinoxConfiguration) environmentInfoTracker.getService();
        configuration.setAppArgs(applicationArguments.toArray(String[]::new));
        environmentInfoTracker.close();
        return configuration;
    }

    private Map<String, String> getFrameworkProperties(Path workDir) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("osgi.configuration.area", workDir.resolve("configuration").toAbsolutePath().toString());
        map.put("osgi.instance.area", workDir.resolve("data").toAbsolutePath().toString());
        map.put("osgi.compatibility.bootdelegation", "true");
        map.put("osgi.framework.useSystemProperties", "false");
        return map;
    }

    Logger getLogger() {
        return logger;
    }

}
