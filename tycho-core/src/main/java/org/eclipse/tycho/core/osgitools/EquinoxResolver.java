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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.internal.resolver.StateImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment.SystemPackageEntry;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

@Component(role = EquinoxResolver.class)
public class EquinoxResolver {
    private static StateObjectFactory factory = StateObjectFactory.defaultFactory;

    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private Logger logger;

    @Requirement
    private ToolchainManager toolchainManager;

    public State newResolvedState(ReactorProject project, MavenSession mavenSession, ExecutionEnvironment ee,
            boolean ignoreEE, DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(project, artifacts, ee);

        State state = newState(artifacts, properties, ignoreEE, mavenSession);

        resolveState(state);

        BundleDescription bundleDescription = state.getBundleByLocation(getNormalizedPath(project.getBasedir()));

        assertResolved(state, bundleDescription);

        return state;
    }

    public State newResolvedState(File basedir, MavenSession mavenSession, ExecutionEnvironment ee,
            DependencyArtifacts artifacts) throws BundleException {
        Properties properties = getPlatformProperties(new Properties(), null, ee);

        State state = newState(artifacts, properties, false, mavenSession);

        resolveState(state);

        BundleDescription bundleDescription = state.getBundleByLocation(getNormalizedPath(basedir));

        assertResolved(state, bundleDescription);

        return state;
    }

    protected void resolveState(State state) {
        state.resolve(false);

        // warn about missing/ambiguous/inconsistent system.bundle
        BundleDescription[] bundles = state.getBundles("system.bundle");
        if (bundles == null || bundles.length == 0) {
            logger.warn("No system.bundle");
        } else if (bundles.length > 1) {
            logger.warn("Multiple system.bundles " + Arrays.toString(bundles));
        } else if (bundles[0].getBundleId() != Constants.SYSTEM_BUNDLE_ID) {
            logger.warn("system.bundle bundleId == " + bundles[0].getBundleId());
        }
    }

    public String toDebugString(State state) {
        StringBuilder sb = new StringBuilder("Resolved OSGi state\n");
        for (BundleDescription otherBundle : state.getBundles()) {
            if (!otherBundle.isResolved()) {
                sb.append("NOT ");
            }
            sb.append("RESOLVED ");
            sb.append(otherBundle.toString()).append(" : ").append(otherBundle.getLocation());
            sb.append('\n');
            for (ResolverError error : state.getResolverErrors(otherBundle)) {
                sb.append('\t').append(error.toString()).append('\n');
            }
        }
        return sb.toString();
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
            properties.put(PlatformPropertiesUtils.OSGI_OS, environment.getOs());
            properties.put(PlatformPropertiesUtils.OSGI_WS, environment.getWs());
            properties.put(PlatformPropertiesUtils.OSGI_ARCH, environment.getArch());
        }

        ExecutionEnvironmentUtils.applyProfileProperties(properties, ee);

        // Put Equinox OSGi resolver into development mode.
        // See http://www.nabble.com/Re:-resolving-partially-p18449054.html
        properties.put(StateImpl.OSGI_RESOLVER_MODE, StateImpl.DEVELOPMENT_MODE);
        return properties;
    }

    protected State newState(DependencyArtifacts artifacts, Properties properties, boolean ignoreEE,
            MavenSession mavenSession) throws BundleException {
        State state = factory.createState(true);

        Map<File, Dictionary<String, String>> systemBundles = new LinkedHashMap<>();
        Map<File, Dictionary<String, String>> externalBundles = new LinkedHashMap<>();
        Map<File, Dictionary<String, String>> projects = new LinkedHashMap<>();

        for (ArtifactDescriptor artifact : artifacts.getArtifacts(ArtifactType.TYPE_ECLIPSE_PLUGIN)) {
            File location = artifact.getLocation(true);
            Dictionary<String, String> mf = loadManifest(location);
            if (isFrameworkImplementation(location, mf)) {
                systemBundles.put(location, mf);
            } else if (artifact.getMavenProject() != null) {
                projects.put(location, mf);
            } else {
                externalBundles.put(location, mf);
            }
        }

        long id = Constants.SYSTEM_BUNDLE_ID;
        if (systemBundles.isEmpty()) {
            // there were no OSGi framework implementations among bundles being resolve
            // fabricate system.bundle to export visible JRE packages
            state.addBundle(factory.createBundleDescription(state, getSystemBundleManifest(properties), "", id++));
        } else {
            // use first framework implementation found as system bundle, i.e. bundleId==0
            // TODO test what happens when multiple framework implementations are present
            for (Map.Entry<File, Dictionary<String, String>> entry : systemBundles.entrySet()) {
                addBundle(state, id++, entry.getKey(), entry.getValue(), false);
            }
        }
        for (Map.Entry<File, Dictionary<String, String>> entry : externalBundles.entrySet()) {
            addBundle(state, id++, entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<File, Dictionary<String, String>> entry : projects.entrySet()) {
            // make sure reactor projects override anything from the target platform
            // that has the same bundle symbolic name
            addBundle(state, id++, entry.getKey(), entry.getValue(), true/* override */);
        }

        List<Dictionary<Object, Object>> allProps = new ArrayList<>();

        // force our system.bundle
        Hashtable<Object, Object> platformProperties = new Hashtable<>(properties);
        platformProperties.put(StateImpl.STATE_SYSTEM_BUNDLE,
                state.getBundle(Constants.SYSTEM_BUNDLE_ID).getSymbolicName());
        allProps.add(platformProperties);
        if (ignoreEE) {
            // ignoring EE by adding all known EEs
            for (String profile : ExecutionEnvironmentUtils.getProfileNames()) {
                StandardExecutionEnvironment executionEnvironment = ExecutionEnvironmentUtils
                        .getExecutionEnvironment(profile, toolchainManager, mavenSession, logger);
                Properties envProps = executionEnvironment.getProfileProperties();
                String execEnv = envProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
                Dictionary<Object, Object> prop = new Hashtable<>();
                // system packages don't exist in EE profiles after Java 11
                if (!executionEnvironment.getSystemPackages().isEmpty()) {
                    prop.put(Constants.FRAMEWORK_SYSTEMPACKAGES, executionEnvironment.getSystemPackages().stream()
                            .map(SystemPackageEntry::toPackageSpecifier).collect(Collectors.joining(",")));
                }
                prop.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, execEnv);
                allProps.add(prop);
            }
        }

        Dictionary<Object, Object>[] stateProperties = allProps.toArray(new Dictionary[allProps.size()]);

        state.setPlatformProperties(stateProperties);

        return state;
    }

    private boolean isFrameworkImplementation(File location, Dictionary<String, String> mf) {
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

    public void addBundle(State state, long id, File bundleLocation, Dictionary<String, String> mf, boolean override)
            throws BundleException {
        BundleDescription descriptor = factory.createBundleDescription(state, mf, getNormalizedPath(bundleLocation),
                id);

        if (override) {
            BundleDescription[] conflicts = state.getBundles(descriptor.getSymbolicName());
            if (conflicts != null) {
                for (BundleDescription conflict : conflicts) {
                    state.removeBundle(conflict);
                    logger.warn(
                            conflict.toString() + " has been replaced by another bundle with the same symbolic name "
                                    + descriptor.toString());
                }
            }
        }

        state.addBundle(descriptor);
    }

    private static String getNormalizedPath(File file) throws BundleException {
        return file.getAbsolutePath();
    }

    private Dictionary<String, String> loadManifest(File bundleLocation) {
        if (bundleLocation == null || !bundleLocation.exists()) {
            throw new IllegalArgumentException("bundleLocation not found: " + bundleLocation);
        }

        return manifestReader.loadManifest(bundleLocation).getHeaders();
    }

    private Dictionary<String, String> getSystemBundleManifest(Properties properties) {
        String systemPackages = properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);

        Dictionary<String, String> systemBundleManifest = new Hashtable<>();
        systemBundleManifest.put(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
        systemBundleManifest.put(Constants.BUNDLE_VERSION, "0.0.0");
        systemBundleManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
        systemBundleManifest.put(StateImpl.Eclipse_JREBUNDLE, "true");
        if (systemPackages != null && systemPackages.trim().length() > 0) {
            systemBundleManifest.put(Constants.EXPORT_PACKAGE, systemPackages);
        } else {
            logger.warn(
                    "Undefined or empty org.osgi.framework.system.packages system property, system.bundle does not export any packages.");
        }

        return systemBundleManifest;
    }

    public void assertResolved(State state, BundleDescription desc) throws BundleException {
        if (!desc.isResolved()) {

            if (logger.isDebugEnabled()) {
                logger.debug("Equinox resolver state:\n" + toDebugString(state));
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Bundle ").append(desc.getSymbolicName()).append(" cannot be resolved\n");
            msg.append("Resolution errors:\n");
            ResolverError[] errors = getResolverErrors(state, desc);
            for (ResolverError error : errors) {
                msg.append("   Bundle ").append(error.getBundle().getSymbolicName()).append(" - ")
                        .append(error.toString()).append("\n");
            }

            throw new BundleException(msg.toString());
        }
    }

    public ResolverError[] getResolverErrors(State state, BundleDescription bundle) {
        Set<ResolverError> errors = new LinkedHashSet<>();
        getRelevantErrors(state, errors, bundle);
        return errors.toArray(new ResolverError[errors.size()]);
    }

    private void getRelevantErrors(State state, Set<ResolverError> errors, BundleDescription bundle) {
        ResolverError[] bundleErrors = state.getResolverErrors(bundle);
        for (ResolverError error : bundleErrors) {
            errors.add(error);
            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                BundleDescription[] requiredBundles = state.getBundles(constraint.getName());
                for (BundleDescription requiredBundle : requiredBundles) {
                    // if one of the constraints is the bundle itself (recursive dependency)
                    // do not handle that bundle (again). See bug 442594.
                    if (bundle.equals(requiredBundle)) {
                        continue;
                    }
                    getRelevantErrors(state, errors, requiredBundle);
                }
            }
        }
    }

}
