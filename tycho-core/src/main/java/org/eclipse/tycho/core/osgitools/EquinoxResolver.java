/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - Bug 567782 - Platform specific fragment not support in Multi-Platform POMless build
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.Settings;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleContainerAdaptor;
import org.eclipse.osgi.container.ModuleDatabase;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder;
import org.eclipse.osgi.container.ModuleWire;
import org.eclipse.osgi.container.SystemModule;
import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

@Component(role = EquinoxResolver.class)
public class EquinoxResolver {
    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private Logger logger;

    @Requirement
    private ToolchainManager toolchainManager;

    public ModuleContainer newResolvedState(ReactorProject project, MavenSession mavenSession, ExecutionEnvironment ee,
            boolean ignoreEE, DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(project, artifacts, ee);
        ModuleContainer container = newState(artifacts, properties, ignoreEE, mavenSession);
        ResolutionReport report = container.resolve(null, false);
        ModuleRevision module = container.getModule(getNormalizedPath(project.getBasedir())).getCurrentRevision();
        if (report.getEntries().isEmpty()) {
            Collection<org.eclipse.osgi.container.Module> toUninstall = null;
            if ((module.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                // other fragments for the same host
                toUninstall = module.getWiring().getRequiredModuleWires(HostNamespace.HOST_NAMESPACE).stream()
                        .map(ModuleWire::getProvider) //
                        .flatMap(host -> host.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream())
                        .map(ModuleWire::getRequirer) //
                        .filter(Predicate.not(module::equals)) //
                        .filter(fragment -> (fragment.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) //
                        .map(revision -> revision.getRevisions().getModule()) //
                        .collect(Collectors.toSet());
            } else {
                // fragments for the host
                toUninstall = module.getWiring().getProvidedModuleWires(HostNamespace.HOST_NAMESPACE).stream()
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                report = container.refresh(null);
            }
        }
        assertResolved(report, module);

        return container;
    }

    public ModuleContainer newResolvedState(File basedir, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(new Properties(), null, ee);
        ModuleContainer container = newState(artifacts, properties, false, mavenSession);
        ResolutionReport report = container.resolve(null, false);

        ModuleRevision bundleDescription = container.getModule(getNormalizedPath(basedir)).getCurrentRevision();
        assertResolved(report, bundleDescription);

        return container;
    }

    protected Properties getPlatformProperties(ReactorProject project, DependencyArtifacts artifacts,
            ExecutionEnvironment ee) {

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        TargetEnvironment environment = configuration.getEnvironments().get(0);
        if (artifacts instanceof MultiEnvironmentDependencyArtifacts) {
            environment = ((MultiEnvironmentDependencyArtifacts) artifacts).getPlatforms().stream().findFirst()
                    .orElse(environment);
        }
        logger.debug("Using TargetEnvironment " + environment.toFilterExpression() + " to create resolver properties");
        Properties properties = new Properties();
        properties.putAll((Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES));

        return getPlatformProperties(properties, environment, ee);
    }

    protected Properties getPlatformProperties(Properties properties, TargetEnvironment environment,
            ExecutionEnvironment ee) {
        if (environment != null) {
            properties.put(EquinoxConfiguration.PROP_OSGI_OS, environment.getOs());
            properties.put(EquinoxConfiguration.PROP_OSGI_WS, environment.getWs());
            properties.put(EquinoxConfiguration.PROP_OSGI_ARCH, environment.getArch());
        }

        ExecutionEnvironmentUtils.applyProfileProperties(properties, ee);

        // Put Equinox OSGi resolver into development mode.
        // See http://www.nabble.com/Re:-resolving-partially-p18449054.html
        properties.put(StateImpl.OSGI_RESOLVER_MODE, StateImpl.DEVELOPMENT_MODE);
        return properties;
    }

    protected ModuleContainer newState(DependencyArtifacts artifacts, Properties properties, boolean ignoreEE,
            MavenSession mavenSession) throws BundleException {
        if (ignoreEE) {
//            // force our system.bundle
//            Map<?, ?> platformProperties = new HashMap<>(properties);
////            platformProperties.put(StateImpl.STATE_SYSTEM_BUNDLE,
////                    state.getBundle(Constants.SYSTEM_BUNDLE_ID).getSymbolicName());
//            // ignoring EE by adding all known EEs
//            for (String profile : ExecutionEnvironmentUtils.getProfileNames()) {
//                StandardExecutionEnvironment executionEnvironment = ExecutionEnvironmentUtils
//                        .getExecutionEnvironment(profile, toolchainManager, mavenSession, logger);
//                Properties envProps = executionEnvironment.getProfileProperties();
//                String execEnv = envProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
//                Dictionary<Object, Object> prop = new Hashtable<>();
//                // system packages don't exist in EE profiles after Java 11
//                if (!executionEnvironment.getSystemPackages().isEmpty()) {
//                    prop.put(Constants.FRAMEWORK_SYSTEMPACKAGES, executionEnvironment.getSystemPackages().stream()
//                            .map(SystemPackageEntry::toPackageSpecifier).collect(Collectors.joining(",")));
//                }
//                prop.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, execEnv);
//                allProps.add(prop);
//            }
            throw new UnsupportedOperationException("ignoreEE=true currently not supported");
        }

        ModuleContainer[] moduleContainerAccessor = new ModuleContainer[1];
        ModuleContainerAdaptor moduleContainerAdaptor = new ModuleContainerAdaptor() {
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
        };
        ModuleDatabase moduleDatabase = new ModuleDatabase(moduleContainerAdaptor);
        ModuleContainer moduleContainer = new ModuleContainer(moduleContainerAdaptor, moduleDatabase);
        moduleContainerAccessor[0] = moduleContainer;

        Map<File, Map<String, String>> systemBundles = new LinkedHashMap<>();
        Map<File, Map<String, String>> externalBundles = new LinkedHashMap<>();
        Map<File, Map<String, String>> projects = new LinkedHashMap<>();

        for (ArtifactDescriptor artifact : artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN)) {
            File location = artifact.getLocation(true);
            Map<String, String> mf = loadManifest(location);
            if (isFrameworkImplementation(mf)) {
                systemBundles.put(location, mf);
            } else if (artifact.getMavenProject() != null) {
                projects.put(location, mf);
            } else {
                externalBundles.put(location, mf);
            }
        }

        String platformCapability = new StringBuilder(EclipsePlatformNamespace.ECLIPSE_PLATFORM_NAMESPACE).append(';')
                .append(EquinoxConfiguration.PROP_OSGI_OS).append('=')
                .append(properties.getProperty(EquinoxConfiguration.PROP_OSGI_OS)).append(';')
                .append(EquinoxConfiguration.PROP_OSGI_WS).append('=')
                .append(properties.getProperty(EquinoxConfiguration.PROP_OSGI_WS)).append(';')
                .append(EquinoxConfiguration.PROP_OSGI_ARCH).append('=')
                .append(properties.getProperty(EquinoxConfiguration.PROP_OSGI_ARCH)).toString();
        if (!systemBundles.isEmpty()) {
            java.util.Map.Entry<File, Map<String, String>> systemBundle = systemBundles.entrySet().iterator().next();
            ModuleRevisionBuilder moduleRevisionBuilder = OSGiManifestBuilderFactory.createBuilder(
                    systemBundle.getValue(), Constants.SYSTEM_BUNDLE_SYMBOLICNAME,
                    properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES),
                    properties.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES) + ',' + platformCapability);
            moduleContainer.install(null, Constants.SYSTEM_BUNDLE_LOCATION, moduleRevisionBuilder,
                    systemBundle.getKey());
        } else {
            ModuleRevisionBuilder moduleRevisionBuilder = OSGiManifestBuilderFactory.createBuilder(
                    Map.of(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME),
                    Constants.SYSTEM_BUNDLE_SYMBOLICNAME, properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES),
                    properties.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES) + ',' + platformCapability);
            moduleContainer.install(null, Constants.SYSTEM_BUNDLE_LOCATION, moduleRevisionBuilder, null);
        }
        for (Map.Entry<File, Map<String, String>> external : externalBundles.entrySet()) {
            moduleContainer.install(null, external.getKey().getAbsolutePath(),
                    OSGiManifestBuilderFactory.createBuilder(external.getValue()), external.getKey());
        }
        for (Map.Entry<File, Map<String, String>> entry : projects.entrySet()) {
            // make sure reactor projects override anything from the target platform
            // that has the same bundle symbolic name
            moduleContainer.install(null, entry.getKey().getAbsolutePath(),
                    OSGiManifestBuilderFactory.createBuilder(entry.getValue()), entry.getKey());
        }

//        Dictionary<Object, Object>[] stateProperties = allProps.toArray(new Dictionary[allProps.size()]);
//
//        state.setPlatformProperties(stateProperties);

        return moduleContainer;
    }

    private boolean isFrameworkImplementation(Map<String, String> mf) {
        // starting with OSGi R4.2, /META-INF/services/org.osgi.framework.launch.FrameworkFactory
        // can be used to detect framework implementation
        // See http://www.osgi.org/javadoc/r4v42/org/osgi/framework/launch/FrameworkFactory.html

        // Assume only framework implementation export org.osgi.framework package
        String value = mf.get(Constants.EXPORT_PACKAGE);
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

    private Map<String, String> loadManifest(File bundleLocation) {
        if (bundleLocation == null || !bundleLocation.exists()) {
            throw new IllegalArgumentException("bundleLocation not found: " + bundleLocation);
        }
        return manifestReader.loadManifest(bundleLocation).getHeaders();
    }

    public void assertResolved(ResolutionReport report, ModuleRevision desc) throws BundleException {
        if (report == null || report.getEntries() == null || report.getEntries().isEmpty()) {
            return;
        }
        List<Entry> errors = report.getEntries().get(desc);
        if (errors != null && !errors.isEmpty()) {
            throw new BundleException("Bundle " + desc.getSymbolicName() + " cannot be resolved:"
                    + report.getResolutionReportMessage(desc));
        }
    }

}
