/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich -  [Bug 567782] Platform specific fragment not support in Multi-Platform POMless build
 *                          [Bug 572481] Tycho does not understand "additional.bundles" directive in build.properties
 *                          [Issue 303] M2E-core build fails with Uses-constraint-violations
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.SystemModule;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.internal.framework.AliasMapper;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.ClasspathEntry.AccessRule;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.ExecutionEnvironment;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

@Named("equinox")
@Singleton
public class EquinoxResolver implements DependenciesResolver {

    public static final String HINT = "equinox";

    private static final String FORCE_KEEP_USES = "First attempt at resolving bundle failed. Trying harder by keeping `uses` information... This may drastically slow down your build!";

    @Inject
    private BundleReader manifestReader;

    @Inject
    private Logger logger;

    @Inject
    private ToolchainManager toolchainManager;

    @Inject
    private BuildPropertiesParser buildPropertiesParser;

    @Inject
    TychoProjectManager projectManager;

    @Inject
    private DependencyComputer dependencyComputer;

    public ModuleContainer newResolvedState(ReactorProject project, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts, Map<Module, ArtifactDescriptor> descriptorLookup) throws BundleException {
        Objects.requireNonNull(artifacts, "DependencyArtifacts can't be null!");
        ScheduledExecutorService executorService = Executors
                .newScheduledThreadPool(EquinoxResolverConfiguration.THREAD_COUNT);
        try {
            return newResolvedState(project, mavenSession, ee, artifacts, executorService,
                    new EquinoxResolverConfiguration(), descriptorLookup);
        } finally {
            executorService.shutdownNow();
        }
    }

    private ModuleContainer newResolvedState(ReactorProject project, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts, ScheduledExecutorService executorService,
            EquinoxResolverConfiguration config, Map<Module, ArtifactDescriptor> descriptorLookup)
            throws BundleException {
        Properties properties = getPlatformProperties(project, mavenSession, ee);
        ModuleContainer container = newState(artifacts, properties, mavenSession, executorService, config,
                descriptorLookup);
        ResolutionReport report = container.resolve(null, false);
        Module module = container.getModule(getNormalizedPath(project.getBasedir()));
        if (module == null) {
            Module systemModule = container.getModule(Constants.SYSTEM_BUNDLE_LOCATION);
            if (project.getBasedir().equals(systemModule.getCurrentRevision().getRevisionInfo())) {
                module = systemModule;
            }
        }
        ModuleRevision moduleRevision = module.getCurrentRevision();
        if (report.getEntries().isEmpty()) {
            Collection<org.eclipse.osgi.container.Module> toUninstall = null;
            if ((moduleRevision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                // other fragments for the same host
                toUninstall = moduleRevision.getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                        .map(ModuleWire::getProvider) //
                        .flatMap(host -> host.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream())
                        .map(ModuleWire::getRequirer) //
                        .filter(Predicate.not(moduleRevision::equals)) //
                        .filter(fragment -> (fragment.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) //
                        .map(revision -> revision.getRevisions().getModule()) //
                        .collect(Collectors.toSet());
            } else {
                // fragments for the host
                toUninstall = moduleRevision.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                        .map(ModuleWire::getRequirer) //
                        .filter(fragment -> (fragment.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) //
                        .map(revision -> revision.getRevisions().getModule()) //
                        .collect(Collectors.toSet());
            }
            if (!toUninstall.isEmpty()) {
                for (Module uninstall : toUninstall) {
                    try {
                        container.uninstall(uninstall);
                    } catch (BundleException e) {
                    }
                }
                report = container.refresh(null);
            }
        }
        if (logger.isDebugEnabled()) {
            Set<Module> unresolvedModules = container.getModules().stream().filter(m -> m.getState() != State.RESOLVED)
                    .collect(Collectors.toSet());
            if (!unresolvedModules.isEmpty()) {
                logger.warn("OSGi state has " + unresolvedModules.size() + " unresolved module(s):");
                logger.debug("The following modules are used to build the current state:");
                for (Module m : container.getModules()) {
                    State state = m.getState();
                    ModuleRevision revision = m.getCurrentRevision();
                    boolean unresolved = unresolvedModules.contains(m);
                    String message = "| " + state + " | " + revision.getSymbolicName() + " (" + revision.getVersion()
                            + ") @ " + m.getLocation();
                    if (unresolved) {
                        logger.warn(message);
                        String reportMessage = report.getResolutionReportMessage(revision);
                        String[] lines = reportMessage.split("\r?\n");
                        for (int i = 1; i < lines.length; i++) {
                            logger.warn("            " + lines[i]);
                        }
                    } else {
                        logger.debug("   " + message);
                    }
                }
            }
        }
        if (moduleRevision.getRevisions().getModule().getState() == State.RESOLVED || report == null
                || report.getEntries() == null || report.getEntries().isEmpty()) {
            return container;
        }
        List<Entry> errors = report.getEntries().get(moduleRevision);
        if (errors == null || errors.isEmpty()) {
            return container;
        }
        if (!config.keepUses) {
            logger.info(FORCE_KEEP_USES);
            return newResolvedState(project, mavenSession, ee, artifacts, executorService,
                    new EquinoxResolverConfiguration(config, true), descriptorLookup);
        }
        throw new BundleException("Bundle " + moduleRevision.getSymbolicName() + " cannot be resolved:"
                + report.getResolutionReportMessage(moduleRevision));
    }

    protected Properties getPlatformProperties(ReactorProject project, MavenSession mavenSession,
            ExecutionEnvironment ee) {

        TargetPlatformConfiguration configuration = projectManager.getTargetPlatformConfiguration(project);
        //FIXME formally we should resolve the configuration for ALL declared environments!
        TargetEnvironment environment = configuration.getEnvironments().get(0);
        logger.debug("Using TargetEnvironment " + environment.toFilterExpression() + " to create resolver properties");
        Properties properties = computeMergedProperties(project.adapt(MavenProject.class), mavenSession);
        return getPlatformProperties(properties, mavenSession, environment, ee);
    }

    protected Properties getPlatformProperties(Properties properties, MavenSession mavenSession,
            TargetEnvironment environment, ExecutionEnvironment ee) {
        if (environment != null) {
            properties.put(EquinoxConfiguration.PROP_OSGI_OS, environment.getOs());
            properties.put(EquinoxConfiguration.PROP_OSGI_WS, environment.getWs());
            properties.put(EquinoxConfiguration.PROP_OSGI_ARCH, environment.getArch());
        }

        if (ee != null) {
            ExecutionEnvironmentUtils.applyProfileProperties(properties, ee);
        } else {
            // ignoring EE by adding all known EEs
            StringJoiner allSystemPackages = new StringJoiner(",");
            StringJoiner allSystemCapabilities = new StringJoiner(",");
            for (String profile : ExecutionEnvironmentUtils.getProfileNames(toolchainManager, mavenSession, logger)) {
                StandardExecutionEnvironment executionEnvironment = ExecutionEnvironmentUtils
                        .getExecutionEnvironment(profile, toolchainManager, mavenSession, logger);
                String currentSystemPackages = (String) executionEnvironment.getProfileProperties()
                        .get(Constants.FRAMEWORK_SYSTEMPACKAGES);
                if (currentSystemPackages != null && !currentSystemPackages.isEmpty()) {
                    allSystemPackages.add(currentSystemPackages);
                }
                String currentSystemCapabilities = (String) executionEnvironment.getProfileProperties()
                        .get(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
                if (currentSystemCapabilities != null && !currentSystemCapabilities.isEmpty()) {
                    allSystemCapabilities.add(currentSystemCapabilities);
                }
            }
            properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES, allSystemPackages.toString());
            properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, allSystemCapabilities.toString());
        }
        return properties;
    }

    protected ModuleContainer newState(DependencyArtifacts artifacts, Properties properties, MavenSession mavenSession,
            ScheduledExecutorService executorService, EquinoxResolverConfiguration config,
            Map<Module, ArtifactDescriptor> descriptorLookup) throws BundleException {
        ModuleContainer[] moduleContainerAccessor = new ModuleContainer[1];
        ModuleContainerAdaptor moduleContainerAdaptor = new ModuleContainerAdaptor() {

            @Override
            public String getProperty(String key) {
                // see https://github.com/eclipse/tycho/issues/213#issuecomment-912547700 for details about what these does
                return switch (key) {
                case "equinox.resolver.revision.batch.size" -> config.batchSize;
                case "equinox.resolver.batch.timeout" -> EquinoxResolverConfiguration.BATCH_TIMEOUT;
                default -> super.getProperty(key);
                };
            }

            @Override
            public void publishModuleEvent(ModuleEvent type, Module module, Module origin) {
                // nothing to do
            }

            @Override
            public void publishContainerEvent(ContainerEvent type, Module module, Throwable error,
                    FrameworkListener... listeners) {
                // nothing to do
            }

            @Override
            public ResolverHookFactory getResolverHookFactory() {
                return triggers -> new ResolverHook() {
                    @Override
                    public void filterSingletonCollisions(BundleCapability singleton,
                            Collection<BundleCapability> collisionCandidates) {
                        // nothing to do
                    }

                    @Override
                    public void filterResolvable(Collection<BundleRevision> candidates) {
                        // nothing to do
                    }

                    @Override
                    public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
                        // nothing to do
                    }

                    @Override
                    public void end() {
                        // nothing to do
                    }
                };
            }

            @Override
            public ModuleCollisionHook getModuleCollisionHook() {
                return (operationType, target, collisionCandidates) -> {
                    // do nothing as collision (same SymbolicName+version) are not supported by Tycho anyway
                };
            }

            @Override
            public SystemModule createSystemModule() {
                return new SystemModule(moduleContainerAccessor[0]) {
                    @Override
                    public Bundle getBundle() {
                        return null;
                    }

                    @Override
                    protected void cleanup(ModuleRevision revision) {
                        // nothing to do
                    }
                };
            }

            @Override
            public Module createModule(String location, long id, EnumSet<Settings> settings, int startlevel) {
                return new Module(id, location, moduleContainerAccessor[0], settings, startlevel) {
                    @Override
                    public Bundle getBundle() {
                        return null;
                    }

                    @Override
                    protected void cleanup(ModuleRevision revision) {
                        // nothing to do
                    }
                };
            }

            @Override
            public ScheduledExecutorService getScheduledExecutor() {
                return executorService;
            }
        };

        ModuleDatabase moduleDatabase = new ModuleDatabase(moduleContainerAdaptor);
        ModuleContainer moduleContainer = new ModuleContainer(moduleContainerAdaptor, moduleDatabase);
        moduleContainerAccessor[0] = moduleContainer;

        Map<File, OsgiManifest> systemBundles = new LinkedHashMap<>();
        Map<File, OsgiManifest> externalBundles = new LinkedHashMap<>();
        Map<File, OsgiManifest> projects = new LinkedHashMap<>();
        Map<File, ArtifactDescriptor> descriptors = new LinkedHashMap<>();

        List<ArtifactDescriptor> list = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);

        for (ArtifactDescriptor artifact : list) {
            File location = artifact.getLocation(true);
            OsgiManifest mf = loadManifest(location, artifact);
            descriptors.put(location, artifact);
            if (isFrameworkImplementation(mf)) {
                systemBundles.put(location, mf);
            } else {
                ReactorProject mavenProject = artifact.getMavenProject();
                if (mavenProject != null) {
                    Collection<String> additionalBundles = buildPropertiesParser.parse(mavenProject)
                            .getAdditionalBundles();
                    if (!additionalBundles.isEmpty()) {
                        List<String> reqb = new ArrayList<>();
                        String value = mf.getValue(Constants.REQUIRE_BUNDLE);
                        if (value != null) {
                            reqb.add(value);
                        }
                        reqb.addAll(additionalBundles.stream().map(b -> b + ";resolution:=optional").toList());
                        mf.getHeaders().put(Constants.REQUIRE_BUNDLE, String.join(",", reqb));
                    }
                    projects.put(location, mf);
                } else {
                    externalBundles.put(location, mf);
                }
            }
        }

        String systemExtraCapabilities = getSystemExtraCapabilities(properties);

        Map<String, String> systemBundleManifest;
        File systemBundleInfo;
        if (!systemBundles.isEmpty()) {
            Map.Entry<File, OsgiManifest> systemBundle = systemBundles.entrySet().iterator().next();
            systemBundleManifest = systemBundle.getValue().getHeaders();
            systemBundleInfo = systemBundle.getKey();
        } else {
            systemBundleManifest = Map.of(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
            systemBundleInfo = null;
        }

        ModuleRevisionBuilder systemBundleRevisionBuilder = OSGiManifestBuilderFactory.createBuilder(
                systemBundleManifest, Constants.SYSTEM_BUNDLE_SYMBOLICNAME,
                properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES), systemExtraCapabilities);
        install(moduleContainer, null, Constants.SYSTEM_BUNDLE_LOCATION, systemBundleRevisionBuilder, systemBundleInfo,
                config, descriptorLookup, descriptors);

        for (Map.Entry<File, OsgiManifest> external : externalBundles.entrySet()) {
            install(moduleContainer, null, external.getKey().getAbsolutePath(),
                    OSGiManifestBuilderFactory.createBuilder(external.getValue().getHeaders()), external.getKey(),
                    config, descriptorLookup, descriptors);
        }
        for (Map.Entry<File, OsgiManifest> entry : projects.entrySet()) {
            // make sure reactor projects override anything from the target platform
            // that has the same bundle symbolic name
            Map<String, String> headers = entry.getValue().getHeaders();
            ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(headers);
            install(moduleContainer, null, entry.getKey().getAbsolutePath(), builder, entry.getKey(), config,
                    descriptorLookup, descriptors);
        }
        return moduleContainer;
    }

    private static Module install(ModuleContainer moduleContainer, Module origin, String location,
            ModuleRevisionBuilder builder, File revisionInfo, EquinoxResolverConfiguration configuration,
            Map<Module, ArtifactDescriptor> descriptorLookup, Map<File, ArtifactDescriptor> descriptors)
            throws BundleException {
        if (!configuration.keepUses) {
            List<GenericInfo> capabilities = builder.getCapabilities();
            for (GenericInfo genericInfo : capabilities) {
                genericInfo.getDirectives().remove("uses");
            }
        }
        Module module = moduleContainer.install(origin, location, builder, revisionInfo);
        ArtifactDescriptor descriptor = descriptors.get(revisionInfo);
        if (descriptor != null) {
            descriptorLookup.put(module, descriptor);
        }
        return module;
    }

    private boolean isFrameworkImplementation(OsgiManifest mf) {
        // starting with OSGi R4.2, /META-INF/services/org.osgi.framework.launch.FrameworkFactory
        // can be used to detect framework implementation
        // See https://www.osgi.org/javadoc/r4v42/org/osgi/framework/launch/FrameworkFactory.html

        // Assume only framework implementation export org.osgi.framework package
        String value = mf.getHeaders().get(Constants.EXPORT_PACKAGE);
        if (value != null) {
            try {
                ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, value);
                for (ManifestElement export : exports) {
                    if ("org.osgi.framework".equals(export.getValue())) {
                        return true;
                    }
                }
            } catch (BundleException e) {
                // fall through
            }
        }
        return false;
    }

    private static String getNormalizedPath(File file) {
        return file.getAbsolutePath();
    }

    private static final AliasMapper ALIAS_MAPPER = new AliasMapper(); // has no state

    /**
     * Heavily based on {@link org.eclipse.osgi.storage.Storage#getSystemExtraCapabilities} and
     * adjusted to what is put in the properties in
     * {@link #getPlatformProperties(ReactorProject, MavenSession, DependencyArtifacts, ExecutionEnvironment)}
     * and its callees.
     */
    private static String getSystemExtraCapabilities(Properties equinoxConfig) {

        StringBuilder result = new StringBuilder();

        String systemCapabilities = equinoxConfig.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
        if (systemCapabilities != null && systemCapabilities.trim().length() > 0) {
            result.append(systemCapabilities).append(", ");
        }

        String os = equinoxConfig.getProperty(EquinoxConfiguration.PROP_OSGI_OS);
        String ws = equinoxConfig.getProperty(EquinoxConfiguration.PROP_OSGI_WS);
        String osArch = equinoxConfig.getProperty(EquinoxConfiguration.PROP_OSGI_ARCH);
        String nl = equinoxConfig.getProperty(EquinoxConfiguration.PROP_OSGI_NL);
        result.append(EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE).append("; ");
        result.append(EquinoxConfiguration.PROP_OSGI_OS).append("=").append(os).append("; ");
        result.append(EquinoxConfiguration.PROP_OSGI_WS).append("=").append(ws).append("; ");
        result.append(EquinoxConfiguration.PROP_OSGI_ARCH).append("=").append(osArch).append("; ");
        result.append(EquinoxConfiguration.PROP_OSGI_NL).append("=").append(nl);

        // For the constants Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_OS_VERSION and Constants.FRAMEWORK_LANGUAGE no value is present.
        // But at least we have the os and osArch from above.
        String osName = os == null ? null : ALIAS_MAPPER.getCanonicalOSName(os);
        String processor = osArch == null ? null : ALIAS_MAPPER.getCanonicalProcessor(osArch);

        result.append(", ").append(NativeNamespace.NATIVE_NAMESPACE);
        if (osName != null) {
            String osNames = getStringList(ALIAS_MAPPER.getOSNameAliases(osName));
            result.append("; ").append(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE).append(osNames);
        }
        if (processor != null) {
            String processors = getStringList(ALIAS_MAPPER.getProcessorAliases(processor));
            result.append("; ").append(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE).append(processors);
        }
        // osVersion and language are not (yet) supported.
        return result.toString();
    }

    private static String getStringList(Collection<String> elements) {
        return ":List<String>=\"" + String.join(",", elements) + "\"";
    }

    private OsgiManifest loadManifest(File bundleLocation, ArtifactDescriptor artifact) {
        if (bundleLocation == null) {
            throw new IllegalArgumentException("bundleLocation can't be null for artifact " + artifact);
        }
        if (!checkExits(bundleLocation)) {
            throw new IllegalArgumentException(
                    "bundleLocation not found: " + bundleLocation + " for artifact " + artifact);
        }
        return manifestReader.loadManifest(bundleLocation);
    }

    private boolean checkExits(File bundleLocation) {
        if (bundleLocation.exists()) {
            return true;
        }
        //especially on windows there can be delayed writes that ruin our day, just to be sure wait a little bit if the file really do not exits!
        long deadLine = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadLine) {
            if (bundleLocation.exists()) {
                return true;
            }
            Thread.onSpinWait();
        }
        return bundleLocation.exists();
    }

    private ModuleContainer getResolverState(ReactorProject project, MavenProject mavenProject,
            DependencyArtifacts artifacts, MavenSession session, Map<Module, ArtifactDescriptor> descriptorLookup) {
        try {
            ExecutionEnvironmentConfiguration eeConfiguration = projectManager
                    .getExecutionEnvironmentConfiguration(mavenProject);
            ExecutionEnvironment executionEnvironment = eeConfiguration.getFullSpecification();
            return newResolvedState(project, session,
                    eeConfiguration.isIgnoredByResolver() ? null : executionEnvironment, artifacts, descriptorLookup);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DependenciesInfo computeDependencies(MavenProject project, DependencyArtifacts artifacts,
            MavenSession session) {
        Map<Module, ArtifactDescriptor> descriptorLookup = new HashMap<>();
        ModuleContainer state = getResolverState(DefaultReactorProject.adapt(project), project, artifacts, session,
                descriptorLookup);
        Module module = state.getModule(project.getBasedir().getAbsolutePath());
        if (module == null) {
            Module systemModule = state.getModule(Constants.SYSTEM_BUNDLE_LOCATION);
            if (project.getBasedir().equals(systemModule.getCurrentRevision().getRevisionInfo())) {
                module = systemModule;
            }
        }
        ModuleRevision bundleDescription = module.getCurrentRevision();

        // dependencies
        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(bundleDescription, revision -> {
            if (revision instanceof ModuleRevision mr) {
                Module key = mr.getRevisions().getModule();
                ArtifactDescriptor descriptor = descriptorLookup.get(key);
                if (descriptor == null) {
                    return new ModuleArtifactDescriptor(key);
                }
                return descriptor;
            } else {
                throw new IllegalArgumentException("Not a valid bundle revision: " + revision);
            }
        });
        return new DependenciesInfo() {

            @Override
            public BundleRevision getRevision() {
                return bundleDescription;
            }

            @Override
            public List<DependencyEntry> getDependencyEntries() {
                return dependencies;
            }

            @Override
            public List<AccessRule> getBootClasspathExtraAccessRules() {
                return dependencyComputer.computeBootClasspathExtraAccessRules(state);
            }
        };
    }

    public static Properties computeMergedProperties(MavenProject mavenProject, MavenSession mavenSession) {
        Properties properties = new Properties();
        properties.putAll(mavenProject.getProperties());
        if (mavenSession != null) {
            properties.putAll(mavenSession.getSystemProperties()); // session wins
            properties.putAll(mavenSession.getUserProperties());
        } else {
            properties.putAll(System.getProperties());
        }
        return properties;
    }
}
