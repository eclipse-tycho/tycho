/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.osgi.framework.BundleException;

@Component(role = EquinoxResolver.class)
public class EquinoxResolver {
    public static final String SYSTEM_BUNDLE_SYMBOLIC_NAME = "system.bundle";

    private static StateObjectFactory factory = StateObjectFactory.defaultFactory;

    @Requirement
    private BundleReader manifestReader;

    @Requirement
    private Logger logger;

    public State newResolvedState(MavenProject project, TargetPlatform platform) throws BundleException {
        Properties properties = getPlatformProperties(project);

        State state = newState(platform, properties);

        resolveState(state);

        BundleDescription bundleDescription = state.getBundleByLocation(getCanonicalPath(project.getBasedir()));

        assertResolved(state, bundleDescription);

        return state;
    }

    public State newResolvedState(File basedir, TargetPlatform platform) throws BundleException {
        Properties properties = getPlatformProperties(new Properties(), null);

        State state = newState(platform, properties);

        resolveState(state);

        BundleDescription bundleDescription = state.getBundleByLocation(getCanonicalPath(basedir));

        assertResolved(state, bundleDescription);

        return state;
    }

    protected void resolveState(State state) {
        state.resolve(false);
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

    protected Properties getPlatformProperties(MavenProject project) {
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        TargetEnvironment environment = configuration.getEnvironments().get(0);

        Properties properties = new Properties();
        properties.putAll((Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES));

        return getPlatformProperties(properties, environment);
    }

    protected Properties getPlatformProperties(Properties properties, TargetEnvironment environment) {
        if (environment != null) {
            properties.put(PlatformPropertiesUtils.OSGI_OS, environment.getOs());
            properties.put(PlatformPropertiesUtils.OSGI_WS, environment.getWs());
            properties.put(PlatformPropertiesUtils.OSGI_ARCH, environment.getArch());
        }

        ExecutionEnvironmentUtils.loadVMProfile(properties);

        // Put Equinox OSGi resolver into development mode.
        // See http://www.nabble.com/Re:-resolving-partially-p18449054.html
        properties.put(org.eclipse.osgi.framework.internal.core.Constants.OSGI_RESOLVER_MODE,
                org.eclipse.osgi.framework.internal.core.Constants.DEVELOPMENT_MODE);
        return properties;
    }

    protected State newState(TargetPlatform platform, Properties properties) throws BundleException {
        State state = factory.createState(true);

        state.setPlatformProperties(properties);

        // add system bundle
        state.addBundle(factory.createBundleDescription(state, getSystemBundleManifest(properties), "", 0));

        long id = 1;

        // make sure reactor projects override anything from target platform
        // that has the same bundle symbolic name
        ArrayList<ArtifactDescriptor> projects = new ArrayList<ArtifactDescriptor>();
        for (ArtifactDescriptor artifact : platform.getArtifacts(ArtifactKey.TYPE_ECLIPSE_PLUGIN)) {
            if (artifact.getMavenProject() != null) {
                projects.add(artifact);
            } else {
                addBundle(state, id++, artifact.getLocation(), false);
            }
        }
        for (ArtifactDescriptor artifact : projects) {
            addBundle(state, id++, artifact.getLocation(), true);
        }
        return state;
    }

    public void addBundle(State state, long id, File bundleLocation, boolean override) throws BundleException {
        if (bundleLocation == null || !bundleLocation.exists()) {
            throw new IllegalArgumentException("bundleLocation not found: " + bundleLocation);
        }

        Dictionary mf = loadManifest(bundleLocation);

        BundleDescription descriptor = factory.createBundleDescription(state, mf, getCanonicalPath(bundleLocation), id);

        if (override) {
            BundleDescription[] conflicts = state.getBundles(descriptor.getSymbolicName());
            if (conflicts != null) {
                for (BundleDescription conflict : conflicts) {
                    state.removeBundle(conflict);
                    logger.warn(conflict.toString()
                            + " has been replaced by another bundle with the same symbolic name "
                            + descriptor.toString());
                }
            }
        }

        state.addBundle(descriptor);
    }

    private static String getCanonicalPath(File file) throws BundleException {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new BundleException(e.getMessage(), e);
        }
    }

    private Dictionary loadManifest(File bundleLocation) {
        return manifestReader.loadManifest(bundleLocation).getHeaders();
    }

    private Dictionary getSystemBundleManifest(Properties properties) {
        String systemPackages = properties.getProperty(org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES);

        Dictionary systemBundleManifest = new Hashtable();
        systemBundleManifest.put(org.eclipse.osgi.framework.internal.core.Constants.BUNDLE_SYMBOLICNAME,
                SYSTEM_BUNDLE_SYMBOLIC_NAME);
        systemBundleManifest.put(org.eclipse.osgi.framework.internal.core.Constants.BUNDLE_VERSION, "0.0.0");
        systemBundleManifest.put(org.eclipse.osgi.framework.internal.core.Constants.BUNDLE_MANIFESTVERSION, "2");
        if (systemPackages != null && systemPackages.trim().length() > 0) {
            systemBundleManifest.put(org.eclipse.osgi.framework.internal.core.Constants.EXPORT_PACKAGE, systemPackages);
        } else {
            logger.warn("Undefined or empty org.osgi.framework.system.packages system property, system.bundle does not export any packages.");
        }

        return systemBundleManifest;
    }

    public void assertResolved(State state, BundleDescription desc) throws BundleException {
        if (!desc.isResolved()) {

            if (logger.isDebugEnabled()) {
                logger.debug("Equinox resolver state:\n" + toDebugString(state));
            }

            StringBuffer msg = new StringBuffer();
            msg.append("Bundle ").append(desc.getSymbolicName()).append(" cannot be resolved\n");
            msg.append("Resolution errors:\n");
            ResolverError[] errors = getResolverErrors(state, desc);
            for (int i = 0; i < errors.length; i++) {
                ResolverError error = errors[i];
                msg.append("   Bundle ").append(error.getBundle().getSymbolicName()).append(" - ")
                        .append(error.toString()).append("\n");
            }

            throw new BundleException(msg.toString());
        }
    }

    public ResolverError[] getResolverErrors(State state, BundleDescription bundle) {
        Set<ResolverError> errors = new LinkedHashSet<ResolverError>();
        getRelevantErrors(state, errors, bundle);
        return (ResolverError[]) errors.toArray(new ResolverError[errors.size()]);
    }

    private void getRelevantErrors(State state, Set<ResolverError> errors, BundleDescription bundle) {
        ResolverError[] bundleErrors = state.getResolverErrors(bundle);
        for (int j = 0; j < bundleErrors.length; j++) {
            ResolverError error = bundleErrors[j];
            errors.add(error);

            VersionConstraint constraint = error.getUnsatisfiedConstraint();
            if (constraint instanceof BundleSpecification || constraint instanceof HostSpecification) {
                BundleDescription[] requiredBundles = state.getBundles(constraint.getName());
                for (int i = 0; i < requiredBundles.length; i++) {
                    getRelevantErrors(state, errors, requiredBundles[i]);
                }
            }
        }
    }

}
