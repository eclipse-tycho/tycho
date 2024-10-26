/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.core.EcJLogFileEnhancer;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.metadata.P2Generator.FileInfo;

import javax.inject.Inject;

@Mojo(name = "p2-metadata", threadSafe = true)
public class P2MetadataMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "project")
    protected MavenProject project;

    @Parameter(defaultValue = "true")
    protected boolean attachP2Metadata;

    @Inject
    protected MavenProjectHelper projectHelper;

    @Inject
    protected P2Generator p2generator;

    /**
     * Project types which this plugin supports.
     */
    @Parameter
    private List<String> supportedProjectTypes = List.of("eclipse-plugin", "eclipse-test-plugin", "eclipse-feature",
            "p2-installable-unit");

    /**
     * Baseline build repository(ies).
     * <p/>
     * P2 assumes that the same artifact type, id and version represent the same artifact. If
     * baselineRepositories parameter is specified, this assumption is validated and optionally
     * enforced.
     */
    @Parameter
    private List<Repository> baselineRepositories;

    /**
     * What happens when build artifact does not match baseline version:
     * <ul>
     * <li><code>disable</code>: Disable baseline validation.</li>
     * <li><code>warn</code> (default): Warn about discrepancies between build and baseline
     * artifacts but do not fail the build.</li>
     * <li><code>failCommon</code>: Fail the build if there are discrepancies between artifacts
     * present both in build and baseline. Attached artifacts only present in the build do not
     * result in build failure.</li>
     * <li><code>fail</code>: Fail the build if there are any discrepancy between build and baseline
     * artifacts.</li>
     * </ul>
     */
    @Parameter(property = "tycho.baseline", defaultValue = "warn")
    private BaselineMode baselineMode;

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

    /**
     * Weather or not detailed information about encountered differences is written in case the
     * comparison found some. The differing states in baseline and build are written to
     * {@code ${project.build.directory}/artifactcomparison}
     */
    @Parameter(property = "tycho.debug.artifactcomparator", defaultValue = "false")
    private boolean writeComparatorDelta;

    /**
     * Whether to replace build artifacts with baseline version or use reactor version:
     * <ul>
     * <li><code>none</code>: Do not replace build artifacts with baseline version.</li>
     * <li><code>common</code>: Replace build artifacts with baseline version. Attached artifacts
     * only present in the build are not removed and will likely result in inconsistencies among
     * artifacts of the same project! Use as last resort when baseline does not contain all build
     * artifacts.</li>
     * <li><code>all</code> (default): Replace build artifacts with baseline version. Attached
     * artifacts only present in the build are removed.</li>
     * </ul>
     */
    @Parameter(property = "tycho.baseline.replace", defaultValue = "all")
    private BaselineReplace baselineReplace;

    /**
     * Whether to generate a 'download.stats' property for artifact metadata. See
     * https://wiki.eclipse.org/Equinox_p2_download_stats
     */
    @Parameter(property = "tycho.generateDownloadStatsProperty", defaultValue = "false")
    private boolean generateDownloadStatsProperty;

    @Inject
    private BaselineValidator baselineValidator;

    @Parameter(property = "tycho.generateChecksums", defaultValue = "true")
    private boolean generateChecksums;

    @Inject
    private IProvisioningAgent agent;

    @Parameter(defaultValue = "false")
    private boolean enhanceLogs;

    /**
     * If given a folder, enhances the ECJ compiler logs with class compare errors so it can be
     * analyzed by tools understanding that format
     */
    @Parameter(defaultValue = "${project.build.directory}/compile-logs")
    private File logDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            attachP2Metadata();
        }
    }

    protected void attachP2Metadata() throws MojoExecutionException {
        if (!attachP2Metadata || !supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }

        File file = project.getArtifact().getFile();

        if (file == null || !file.canRead()) {
            throw new IllegalStateException();
        }

        agent.getService(Object.class); //needed to make checksum computation work see https://github.com/eclipse-equinox/p2/issues/214

        try {
            Map<String, IP2Artifact> generatedMetadata = p2generator.generateMetadata(project,
                    generateDownloadStatsProperty, generateChecksums);

            if (baselineMode != BaselineMode.disable) {
                ComparisonData data = new ComparisonData(ignoredPatterns, writeComparatorDelta);
                if (enhanceLogs && logDirectory != null && logDirectory.isDirectory()) {
                    try {
                        try (EcJLogFileEnhancer enhancer = EcJLogFileEnhancer.create(logDirectory)) {
                            generatedMetadata = baselineValidator.validateAndReplace(project, data, generatedMetadata,
                                    baselineRepositories, baselineMode, baselineReplace, enhancer);
                        }
                    } catch (IOException e) {
                        getLog().warn("Can't enhance logs in directory " + logDirectory);
                    }
                } else {
                    generatedMetadata = baselineValidator.validateAndReplace(project, data, generatedMetadata,
                            baselineRepositories, baselineMode, baselineReplace, null);
                }
            }

            FileInfo info = p2generator.persistMetadata(generatedMetadata, project);
            attachArtifact(project, TychoConstants.EXTENSION_P2_METADATA, TychoConstants.CLASSIFIER_P2_METADATA,
                    info.metadata());
            attachArtifact(project, TychoConstants.EXTENSION_P2_ARTIFACTS, TychoConstants.CLASSIFIER_P2_ARTIFACTS,
                    info.artifacts());

            ReactorProject reactorProject = DefaultReactorProject.adapt(project);

            Set<IInstallableUnit> installableUnits = new LinkedHashSet<>();
            generatedMetadata.forEach((classifier, p2artifact) -> {
                installableUnits.addAll(p2artifact.getInstallableUnits());

                // attach any new classified artifacts, like feature root files for example
                if (classifier != null && !hasAttachedArtifact(project, classifier)) {
                    projectHelper.attachArtifact(project, getExtension(p2artifact.getLocation()), classifier,
                            p2artifact.getLocation());
                }
            });

            // TODO 353889 distinguish between dependency resolution seed units ("primary") and other units of the project
            reactorProject.setDependencyMetadata(DependencyMetadataType.SEED, installableUnits);
            reactorProject.setDependencyMetadata(DependencyMetadataType.RESOLVE, Collections.emptySet());
            p2generator.writeArtifactLocations(project);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not generate P2 metadata", e);
        }
    }

    /**
     * Performs an add or replace of the specified artifact, even though javadoc of
     * {@link MavenProjectHelper} claims it can replace artifacts that generates a warning in recent
     * maven versions.
     */
    private void attachArtifact(MavenProject project, String type, String classifier, File file) {
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (classifier.equals(artifact.getClassifier()) && type.equals(artifact.getType())) {
                artifact.setFile(file);
                return;
            }
        }
        projectHelper.attachArtifact(project, type, classifier, file);
    }

    private static boolean hasAttachedArtifact(MavenProject project, String classifier) {
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (classifier.equals(artifact.getClassifier())) {
                return true;
            }
        }
        return false;
    }

    private static String getExtension(File file) {
        String fileName = file.getName();
        int separator = fileName.lastIndexOf('.');
        if (separator < 0) {
            throw new IllegalArgumentException("No file extension in \"" + fileName + "\"");
        }
        return fileName.substring(separator + 1);
    }

}
