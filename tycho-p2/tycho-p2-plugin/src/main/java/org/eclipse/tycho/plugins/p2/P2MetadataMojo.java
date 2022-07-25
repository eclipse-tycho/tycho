/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;

@Mojo(name = "p2-metadata", threadSafe = true)
public class P2MetadataMojo extends AbstractMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "project")
    protected MavenProject project;

    @Parameter(property = "mojoExecution", readonly = true)
    protected MojoExecution execution;

    @Parameter(defaultValue = "true")
    protected boolean attachP2Metadata;

    @Component
    protected MavenProjectHelper projectHelper;

    @Component(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory equinox;

    /**
     * Project types which this plugin supports.
     */
    @Parameter
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin",
            "eclipse-feature", "p2-installable-unit");

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

    @Component
    private BaselineValidator baselineValidator;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            attachP2Metadata();
        }
    }

    private <T> T getService(Class<T> type) {
        T service = equinox.getService(type);
        if (service == null) {
            throw new IllegalStateException("Could not acquire service " + type);
        }
        return service;
    }

    protected void attachP2Metadata() throws MojoExecutionException {
        if (!attachP2Metadata || !supportedProjectTypes.contains(project.getPackaging())) {
            return;
        }

        File file = project.getArtifact().getFile();

        if (file == null || !file.canRead()) {
            throw new IllegalStateException();
        }

        File targetDir = new File(project.getBuild().getDirectory());

        ArtifactFacade projectDefaultArtifact = new ArtifactFacade(project.getArtifact());

        try {
            List<IArtifactFacade> artifacts = new ArrayList<>();

            artifacts.add(projectDefaultArtifact);

            for (Artifact attachedArtifact : project.getAttachedArtifacts()) {
                if (attachedArtifact.getFile() != null && (attachedArtifact.getFile().getName().endsWith(".jar")
                        || (attachedArtifact.getFile().getName().endsWith(".zip")
                                && project.getPackaging().equals(ArtifactType.TYPE_INSTALLABLE_UNIT)))) {
                    artifacts.add(new ArtifactFacade(attachedArtifact));
                }
            }

            P2Generator p2generator = getService(P2Generator.class);

            Map<String, IP2Artifact> generatedMetadata = p2generator.generateMetadata(artifacts,
                    new PublisherOptions(generateDownloadStatsProperty), targetDir);

            if (baselineMode != BaselineMode.disable) {
                generatedMetadata = baselineValidator.validateAndReplace(project, execution, generatedMetadata,
                        baselineRepositories, baselineMode, baselineReplace);
            }

            File contentsXml = new File(targetDir, TychoConstants.FILE_NAME_P2_METADATA);
            File artifactsXml = new File(targetDir, TychoConstants.FILE_NAME_P2_ARTIFACTS);
            p2generator.persistMetadata(generatedMetadata, contentsXml, artifactsXml);
            projectHelper.attachArtifact(project, TychoConstants.EXTENSION_P2_METADATA,
                    TychoConstants.CLASSIFIER_P2_METADATA, contentsXml);
            projectHelper.attachArtifact(project, TychoConstants.EXTENSION_P2_ARTIFACTS,
                    TychoConstants.CLASSIFIER_P2_ARTIFACTS, artifactsXml);

            ReactorProject reactorProject = DefaultReactorProject.adapt(project);

            Set<Object> installableUnits = new LinkedHashSet<>();
            for (Map.Entry<String, IP2Artifact> entry : generatedMetadata.entrySet()) {
                String classifier = entry.getKey();
                IP2Artifact p2artifact = entry.getValue();

                installableUnits.addAll(p2artifact.getInstallableUnits());

                // attach any new classified artifacts, like feature root files for example
                if (classifier != null && !hasAttachedArtifact(project, classifier)) {
                    projectHelper.attachArtifact(project, getExtension(p2artifact.getLocation()), classifier,
                            p2artifact.getLocation());
                }
            }

            // TODO 353889 distinguish between dependency resolution seed units ("primary") and other units of the project
            reactorProject.setDependencyMetadata(DependencyMetadataType.SEED, installableUnits);
            reactorProject.setDependencyMetadata(DependencyMetadataType.RESOLVE, Collections.emptySet());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not generate P2 metadata", e);
        }

        File localArtifactsFile = new File(project.getBuild().getDirectory(), TychoConstants.FILE_NAME_LOCAL_ARTIFACTS);
        writeArtifactLocations(localArtifactsFile, getAllProjectArtifacts(project));
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

    /**
     * Returns a map from classifiers to artifact files of the given project. The classifier
     * <code>null</code> is mapped to the project's main artifact.
     */
    private static Map<String, File> getAllProjectArtifacts(MavenProject project) {
        Map<String, File> artifacts = new HashMap<>();
        Artifact mainArtifact = project.getArtifact();
        if (mainArtifact != null) {
            artifacts.put(null, mainArtifact.getFile());
        }
        for (Artifact attachedArtifact : project.getAttachedArtifacts()) {
            artifacts.put(attachedArtifact.getClassifier(), attachedArtifact.getFile());
        }
        return artifacts;
    }

    static void writeArtifactLocations(File outputFile, Map<String, File> artifactLocations)
            throws MojoExecutionException {
        Properties outputProperties = new Properties();

        for (Entry<String, File> entry : artifactLocations.entrySet()) {
            if (entry.getKey() == null) {
                outputProperties.put(TychoConstants.KEY_ARTIFACT_MAIN, entry.getValue().getAbsolutePath());
            } else {
                outputProperties.put(TychoConstants.KEY_ARTIFACT_ATTACHED + entry.getKey(),
                        entry.getValue().getAbsolutePath());
            }
        }

        writeProperties(outputProperties, outputFile);
    }

    private static void writeProperties(Properties properties, File outputFile) throws MojoExecutionException {
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outputFile);

            try {
                properties.store(outputStream, null);
            } finally {
                outputStream.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing " + outputFile, e);
        }
    }
}
