/*******************************************************************************
 * Copyright (c) 2015, 2022 Red Hat Inc., and others
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.plugins.p2.extras;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.exceptions.VersionBumpRequiredException;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.target.facade.TargetPlatformFactory;
import org.osgi.framework.Version;

import javax.inject.Inject;

/**
 * This mojo compares versions the output artifacts of your module build with the version of the
 * same artifacts available in configured baselines, in order to detect version inconsistencies
 * (version moved back, or not correctly bumped since last release).
 *
 * Rules for "illegal" versions are:
 * <li>version decreased compared to baseline</li>
 * <li>same fully-qualified version as baseline, but with different binary content</li>
 * <li>same major.minor.micro as baseline, with different qualifier (at least micro should be
 * increased)</li>
 *
 * This mojo doesn't allow to use qualifier as a versioning segment and will most likely drive to
 * false-positive errors if your qualifier has means to show versioniterations.
 *
 * @author mistria
 */
@Mojo(defaultPhase = LifecyclePhase.VERIFY, requiresProject = false, name = "compare-version-with-baselines", threadSafe = true)
public class CompareWithBaselineMojo extends AbstractMojo {

    public static enum ReportBehavior {
        fail, warn
    }

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "mojoExecution", readonly = true)
    protected MojoExecution execution;

    /**
     * A list of p2 repositories to be used as baseline. Those are typically the most recent
     * released versions of your project.
     */
    @Parameter(property = "baselines", name = "baselines")
    private List<String> baselines;

    /**
     * A list of file path patterns that are ignored when comparing the build artifact against the
     * baseline version.
     *
     * {@code
     * <ignoredPatterns>
     *   <pattern>META-INF/ECLIPSE_.RSA<pattern>
     *   <pattern>META-INF/ECLIPSE_.SF</pattern>
     * </ignoredPatterns>
     * }
     *
     */
    @Parameter
    private List<String> ignoredPatterns;

    @Parameter(property = "skip", alias = "tycho.p2.baseline.skip")
    private boolean skip;

    @Parameter(property = "onIllegalVersion", defaultValue = "fail", alias = "tycho.p2.baseline.onIllegalVersion")
    private ReportBehavior onIllegalVersion;

    @Inject
    private P2ResolverFactory resolverFactory;

    @Inject
    private TychoProjectManager projectManager;

    @Inject
    private TargetPlatformFactory platformFactory;

    /**
     * The hint of an available {@link ArtifactComparator} component to use for comparison of
     * artifacts with same version.
     */
    @Parameter(defaultValue = BytesArtifactComparator.HINT)
    private String comparator;

    @Inject
    protected Map<String, ArtifactComparator> artifactComparators;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Execution was skipped");
            return;
        }
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        Set<IInstallableUnit> dependencyMetadata = reactorProject.getDependencyMetadata(DependencyMetadataType.SEED);
        if (dependencyMetadata == null || dependencyMetadata.isEmpty()) {
            getLog().debug("Skipping baseline version comparison, no p2 artifacts created in build");
            return;
        }
        if (!this.artifactComparators.containsKey(this.comparator)) {
            throw new MojoExecutionException("Unknown comparator `" + this.comparator + "`. Known comparators are: "
                    + this.artifactComparators.keySet());
        }

        TargetEnvironment runningEnvironment = TargetEnvironment.getRunningEnvironment();
        P2Resolver resolver = resolverFactory.createResolver(Collections.singletonList(runningEnvironment));

        TargetPlatformConfigurationStub baselineTPStub = new TargetPlatformConfigurationStub();
        baselineTPStub.setIgnoreLocalArtifacts(true);
        baselineTPStub.setEnvironments(Collections.singletonList(runningEnvironment));
        for (String baselineRepo : this.baselines) {
            baselineTPStub.addP2Repository(toRepoURI(baselineRepo));
        }
        ExecutionEnvironmentConfiguration eeConfiguration = projectManager
                .getExecutionEnvironmentConfiguration(project);
        TargetPlatform baselineTP = platformFactory.createTargetPlatform(baselineTPStub, eeConfiguration, null);

        for (IInstallableUnit item : dependencyMetadata) {
            try {
                String id = item.getId();
                Version version = new Version(item.getVersion().toString());
                P2ResolutionResult res = resolver.resolveInstallableUnit(baselineTP, id, "0.0.0");

                for (Entry foundInBaseline : res.getArtifacts()) {
                    Version baselineVersion = new Version(foundInBaseline.getVersion());
                    String versionDelta = "" + (version.getMajor() - baselineVersion.getMajor()) + "."
                            + (version.getMinor() - baselineVersion.getMinor()) + "."
                            + (version.getMicro() - baselineVersion.getMicro());

                    getLog().debug("Found " + foundInBaseline.getId() + "/" + foundInBaseline.getVersion()
                            + " with delta: " + versionDelta);
                    if (version.compareTo(baselineVersion) < 0) {
                        String message = "Version has moved backwards for (" + id + "/" + version + "). Baseline has "
                                + baselineVersion + ") with delta: " + versionDelta;
                        if (this.onIllegalVersion == ReportBehavior.warn) {
                            getLog().warn(message);
                            return;
                        } else {
                            throw new MojoFailureException(message);
                        }
                    } else if (version.equals(baselineVersion)) {
                        File baselineFile = foundInBaseline.getLocation(true);
                        File reactorFile = null;
                        // TODO: currently, there are only 2 kinds of (known) artifacts, but we could have more
                        // and unknown ones. Need to find something smarter to map artifact with actual file.
                        if (id.endsWith("source")) {
                            reactorFile = reactorProject.getArtifact("sources");
                        } else if (id.endsWith("source.feature.jar")) {
                            reactorFile = reactorProject.getArtifact("sources-feature");
                        } else if (id.endsWith("_root")) {
                            reactorFile = reactorProject.getArtifact("root");
                        } else {
                            reactorFile = reactorProject.getArtifact();
                        }
                        ComparisonData data = new ComparisonData(ignoredPatterns, false);
                        ArtifactDelta artifactDelta = this.artifactComparators.get(this.comparator)
                                .getDelta(baselineFile, reactorFile, data);
                        String message = "Baseline and reactor have the same fully qualified version, but different content";
                        if (artifactDelta != null) {
                            if (this.onIllegalVersion == ReportBehavior.warn) {
                                getLog().warn(message);
                                getLog().warn(artifactDelta.getDetailedMessage());
                                return;
                            } else {
                                message += "\n";
                                message += artifactDelta.getDetailedMessage();
                                getLog().error(message);
                                throw new MojoFailureException(message);
                            }
                        }
                    } else if (version.getMajor() == baselineVersion.getMajor()
                            && version.getMinor() == baselineVersion.getMinor()
                            && version.getMicro() == baselineVersion.getMicro()) {
                        String message = "Only qualifier changed for (" + id + "/" + version
                                + "). Expected to have bigger x.y.z than what is available in baseline ("
                                + baselineVersion + ")";
                        if (this.onIllegalVersion == ReportBehavior.warn) {
                            getLog().warn(message);
                            return;
                        } else {
                            throw new VersionBumpRequiredException(message, item, version, baselineVersion);
                        }
                    }
                }

            } catch (Exception ex) {
                throw new MojoFailureException(ex.getMessage(), ex);
            }
        }
    }

    private URI toRepoURI(String s) throws MojoExecutionException {
        if (new File(s).exists()) {
            return new File(s).toURI();
        }
        if (new File(project.getBasedir(), s).exists()) {
            return new File(project.getBasedir(), s).toURI();
        }
        try {
            return new URI(s);
        } catch (Exception ex) {
            throw new MojoExecutionException("Wrong baseline: '" + s + "'", ex);
        }
    }

}
