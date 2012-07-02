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
package org.eclipse.tycho.plugins.p2;

import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.CLASSIFIER_P2_METADATA;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.EXTENSION_P2_METADATA;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_P2_METADATA;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_MAIN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.facade.internal.ArtifactFacade;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;

/**
 * @goal p2-metadata
 */
public class P2MetadataMojo extends AbstractMojo {
    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean attachP2Metadata;

    /** @component */
    protected MavenProjectHelper projectHelper;

    /** @component */
    private EquinoxServiceFactory equinox;

    /**
     * Project types which this plugin supports.
     * 
     * @parameter
     */
    private List<String> supportedProjectTypes = Arrays.asList("eclipse-plugin", "eclipse-test-plugin",
            "eclipse-feature");

    /**
     * Baseline build repository(ies).
     * <p/>
     * P2 assumes that the same artifact type, id and version represent the same artifact. If
     * baselineRepositories parameter is specified, this assumption is validated and optionally
     * enforced.
     * 
     * @parameter
     */
    private List<Repository> baselineRepositories;

    /**
     * What happens when build artifact does not match baseline version.
     * 
     * @parameter expression="${tycho.baseline}" default=value="fail"
     */
    private BaselineMode baselineMode;

    /**
     * Whether to replace build artifacts with baseline version or use reactor version.
     * 
     * @parameter expression="${tycho.baseline.replace}" default-value="all"
     */
    private BaselineReplace baselineReplace;

    /**
     * @component
     */
    private BaselineValidator baselineValidator;

    public void execute() throws MojoExecutionException, MojoFailureException {
        attachP2Metadata();
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
            List<IArtifactFacade> artifacts = new ArrayList<IArtifactFacade>();

            artifacts.add(projectDefaultArtifact);

            for (Artifact attachedArtifact : project.getAttachedArtifacts()) {
                if (attachedArtifact.getFile() != null && attachedArtifact.getFile().getName().endsWith(".jar")) {
                    artifacts.add(new ArtifactFacade(attachedArtifact));
                }
            }

            P2Generator p2generator = getService(P2Generator.class);

            Map<String, IP2Artifact> generatedMetadata = p2generator.generateMetadata(artifacts, targetDir);

            if (baselineMode != BaselineMode.disable) {
                generatedMetadata = baselineValidator.validateAndReplace(project, generatedMetadata,
                        baselineRepositories, baselineMode, baselineReplace);
            }

            File contentsXml = new File(targetDir, FILE_NAME_P2_METADATA);
            File artifactsXml = new File(targetDir, FILE_NAME_P2_ARTIFACTS);
            p2generator.persistMetadata(generatedMetadata, contentsXml, artifactsXml);
            projectHelper.attachArtifact(project, EXTENSION_P2_METADATA, CLASSIFIER_P2_METADATA, contentsXml);
            projectHelper.attachArtifact(project, EXTENSION_P2_ARTIFACTS, CLASSIFIER_P2_ARTIFACTS, artifactsXml);

            ReactorProject reactorProject = DefaultReactorProject.adapt(project);

            for (Map.Entry<String, IP2Artifact> entry : generatedMetadata.entrySet()) {
                String classifier = entry.getKey();
                IP2Artifact p2artifact = entry.getValue();

                reactorProject.setDependencyMetadata(classifier, true, p2artifact.getInstallableUnits());
                reactorProject.setDependencyMetadata(classifier, false, Collections.emptySet());

                // attach any new classified artifacts, like feature root files for example
                if (classifier != null && !hasAttachedArtifact(project, classifier)) {
                    projectHelper.attachArtifact(project, getExtension(p2artifact.getLocation()), classifier,
                            p2artifact.getLocation());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not generate P2 metadata", e);
        }

        File localArtifactsFile = new File(project.getBuild().getDirectory(), FILE_NAME_LOCAL_ARTIFACTS);
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
        Map<String, File> artifacts = new HashMap<String, File>();
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
                outputProperties.put(KEY_ARTIFACT_MAIN, entry.getValue().getAbsolutePath());
            } else {
                outputProperties.put(KEY_ARTIFACT_ATTACHED + entry.getKey(), entry.getValue().getAbsolutePath());
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
