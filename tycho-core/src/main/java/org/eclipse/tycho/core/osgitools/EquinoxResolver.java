/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich -  [Bug 567782] Platform specific fragment not support in Multi-Platform POMless build
 *                          [Bug 572481] Tycho does not understand "additional.bundles" directive in build.properties
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.osgi.container.Module.State;
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
import org.eclipse.osgi.internal.framework.AliasMapper;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.RequiredCapability;
import org.eclipse.tycho.Requirements;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.VersionRange;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

@Component(role = EquinoxResolver.class)
public class EquinoxResolver {
    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Requirement
    private Logger logger;

    @Requirement
    private ToolchainManager toolchainManager;

    public ModuleContainer newResolvedState(ReactorProject project, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(project, mavenSession, artifacts, ee);
        ModuleContainer container = newState(artifacts, properties, mavenSession);
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                report = container.refresh(null);
            }
        }
        assertResolved(report, moduleRevision);

        return container;
    }

    public ModuleContainer newResolvedState(File basedir, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(new Properties(), mavenSession, null, ee);
        ModuleContainer container = newState(artifacts, properties, mavenSession);
        ResolutionReport report = container.resolve(null, false);

        ModuleRevision bundleDescription = container.getModule(getNormalizedPath(basedir)).getCurrentRevision();
        assertResolved(report, bundleDescription);

        return container;
    }

    protected Properties getPlatformProperties(ReactorProject project, MavenSession mavenSession,
            DependencyArtifacts artifacts, ExecutionEnvironment ee) {

        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        TargetEnvironment environment = configuration.getEnvironments().get(0);
        if (artifacts instanceof MultiEnvironmentDependencyArtifacts) {
            environment = ((MultiEnvironmentDependencyArtifacts) artifacts).getPlatforms().stream().findFirst()
                    .orElse(environment);
        }
        logger.debug("Using TargetEnvironment " + environment.toFilterExpression() + " to create resolver properties");
        Properties properties = new Properties();
        properties.putAll((Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES));

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
            StringBuilder allSystemPackages = new StringBuilder();
            StringBuilder allSystemCapabilities = new StringBuilder();
            for (String profile : ExecutionEnvironmentUtils.getProfileNames()) {
                StandardExecutionEnvironment executionEnvironment = ExecutionEnvironmentUtils
                        .getExecutionEnvironment(profile, toolchainManager, mavenSession, logger);
                String currentSystemPackages = (String) executionEnvironment.getProfileProperties()
                        .get(Constants.FRAMEWORK_SYSTEMPACKAGES);
                if (currentSystemPackages != null && !currentSystemPackages.isEmpty()) {
                    allSystemPackages.append(currentSystemPackages);
                    allSystemPackages.append(',');
                }
                String currentSystemCapabilities = (String) executionEnvironment.getProfileProperties()
                        .get(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
                if (currentSystemCapabilities != null && !currentSystemCapabilities.isEmpty()) {
                    allSystemCapabilities.append(currentSystemCapabilities);
                    allSystemCapabilities.append(',');
                }
            }
            if (allSystemPackages.length() > 0) {
                allSystemPackages.deleteCharAt(allSystemPackages.length() - 1);
            }
            properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES, allSystemPackages.toString());
            if (allSystemCapabilities.length() > 0) {
                allSystemCapabilities.deleteCharAt(allSystemCapabilities.length() - 1);
            }
            properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, allSystemCapabilities.toString());
        }
        return properties;
    }

    protected ModuleContainer newState(DependencyArtifacts artifacts, Properties properties, MavenSession mavenSession)
            throws BundleException {
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

        Map<File, OsgiManifest> systemBundles = new LinkedHashMap<>();
        Map<File, OsgiManifest> externalBundles = new LinkedHashMap<>();
        Map<File, OsgiManifest> projects = new LinkedHashMap<>();
        Map<File, List<RequiredCapability>> requirementsMap = new LinkedHashMap<>();

        List<ArtifactDescriptor> list = artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN);
        for (ArtifactDescriptor artifact : list) {
            File location = artifact.getLocation(true);
            OsgiManifest mf = loadManifest(location);
            if (isFrameworkImplementation(mf)) {
                systemBundles.put(location, mf);
            } else {
                ReactorProject mavenProject = artifact.getMavenProject();
                if (mavenProject != null) {
                    Collection<String> additionalBundles = buildPropertiesParser.parse(mavenProject.getBasedir())
                            .getAdditionalBundles();
                    if (additionalBundles.size() > 0) {
                        List<String> reqb = new ArrayList<>();
                        String value = mf.getValue(Constants.REQUIRE_BUNDLE);
                        if (value != null) {
                            reqb.add(value);
                        }
                        reqb.addAll(additionalBundles.stream().map(b -> b + ";resolution:=optional")
                                .collect(Collectors.toList()));
                        mf.getHeaders().put(Constants.REQUIRE_BUNDLE, String.join(",", reqb));
                    }
                    projects.put(location, mf);
                    Requirements requirements = mavenProject.getRequirements();
                    if (requirements != null) {
                        requirementsMap.put(location,
                                requirements.getRequiredCapabilities(mavenProject, DependencyMetadataType.COMPILE));
                    }
                } else {
                    externalBundles.put(location, mf);
                }
            }
        }

        String systemExtraCapabilities = getSystemExtraCapabilities(properties);

        Map<String, String> systemBundleManifest;
        Object systemBundleInfo;
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

        moduleContainer.install(null, Constants.SYSTEM_BUNDLE_LOCATION, systemBundleRevisionBuilder, systemBundleInfo);

        for (Map.Entry<File, OsgiManifest> external : externalBundles.entrySet()) {
            moduleContainer.install(null, external.getKey().getAbsolutePath(),
                    OSGiManifestBuilderFactory.createBuilder(external.getValue().getHeaders()), external.getKey());
        }
        for (Map.Entry<File, OsgiManifest> entry : projects.entrySet()) {
            // make sure reactor projects override anything from the target platform
            // that has the same bundle symbolic name
            Map<String, String> headers = entry.getValue().getHeaders();
            ModuleRevisionBuilder builder = OSGiManifestBuilderFactory.createBuilder(headers);
            List<RequiredCapability> cap = requirementsMap.get(entry.getKey());
            if (cap != null) {
                for (RequiredCapability requiredCapability : cap) {
                    StringBuilder filter = new StringBuilder();
                    VersionRange range = new VersionRange(requiredCapability.getVersionRange());
                    filter.append("(&(").append(BundleNamespace.BUNDLE_NAMESPACE).append('=')
                            .append(requiredCapability.getId()).append(')')
                            .append(range.toFilterString(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE))
                            .append(')');
                    builder.addRequirement(BundleNamespace.BUNDLE_NAMESPACE,
                            Collections.singletonMap(BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()),
                            Collections.emptyMap());
                }
            }
            moduleContainer.install(null, entry.getKey().getAbsolutePath(), builder, entry.getKey());
        }

        return moduleContainer;
    }

    private boolean isFrameworkImplementation(OsgiManifest mf) {
        // starting with OSGi R4.2, /META-INF/services/org.osgi.framework.launch.FrameworkFactory
        // can be used to detect framework implementation
        // See http://www.osgi.org/javadoc/r4v42/org/osgi/framework/launch/FrameworkFactory.html

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

    private OsgiManifest loadManifest(File bundleLocation) {
        if (bundleLocation == null || !bundleLocation.exists()) {
            throw new IllegalArgumentException("bundleLocation not found: " + bundleLocation);
        }
        return manifestReader.loadManifest(bundleLocation);
    }

    public void assertResolved(ResolutionReport report, ModuleRevision desc) throws BundleException {
        if (desc.getRevisions().getModule().getState() == State.RESOLVED || report == null
                || report.getEntries() == null || report.getEntries().isEmpty()) {
            return;
        }
        List<Entry> errors = report.getEntries().get(desc);
        if (errors != null && !errors.isEmpty()) {
            throw new BundleException("Bundle " + desc.getSymbolicName() + " cannot be resolved:"
                    + report.getResolutionReportMessage(desc));
        }
    }

}
