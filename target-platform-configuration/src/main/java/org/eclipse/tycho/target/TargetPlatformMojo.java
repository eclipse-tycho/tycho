/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.ReactorProjectIdentities;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManagerFacade;

/**
 * @goal target-platform
 */
public class TargetPlatformMojo extends AbstractMojo {

    // TODO site doc (including steps & parameters handled in afterProjectsRead?)
    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /** @component */
    private EquinoxServiceFactory osgiServices;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ReactorRepositoryManagerFacade repositoryManager = osgiServices
                .getService(ReactorRepositoryManagerFacade.class);
        List<ReactorProjectIdentities> upstreamProjects = getReferencedTychoProjects();
        repositoryManager.computeFinalTargetPlatform(DefaultReactorProject.adapt(project), upstreamProjects);
    }

    private List<ReactorProjectIdentities> getReferencedTychoProjects() throws MojoExecutionException {
        List<ReactorProjectIdentities> result = new ArrayList<ReactorProjectIdentities>();
        for (MavenProject reactorProject : project.getProjectReferences().values()) {

            // check for target platform relevant build results (registered by either p2-metadata-default or attach-artifacts)
            File metadataXml = getAttachedArtifact(reactorProject, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA);
            if (metadataXml == null) {
                continue;
            }
            File artifactXml = getAttachedArtifact(reactorProject, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS);

            ReactorProject tychoReactorProject = DefaultReactorProject.adapt(reactorProject);
            verifyIndexFileLocations(tychoReactorProject, metadataXml, artifactXml);
            result.add(tychoReactorProject.getIdentities());
        }
        return result;
    }

    private static File getAttachedArtifact(MavenProject project, String classifier) {
        for (Artifact artifact : project.getAttachedArtifacts()) {
            if (classifier.equals(artifact.getClassifier())) {
                return artifact.getFile();
            }
        }
        return null;
    }

    private static void verifyIndexFileLocations(ReactorProject project, File metadataXml, File artifactXml)
            throws MojoExecutionException {
        verifyArtifactLocationInTargetFolder(project, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                RepositoryLayoutHelper.FILE_NAME_P2_METADATA, metadataXml);
        verifyArtifactLocationInTargetFolder(project, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS,
                RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS, artifactXml);
        verifyFilePresenceInTargetFolder(project, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
    }

    private static void verifyArtifactLocationInTargetFolder(ReactorProject project, String artifactClassifier,
            String expectedPathInTarget, File actualLocation) throws MojoExecutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (actualLocation == null) {
            throw new MojoExecutionException("Unexpected build result of " + project + ": Artifact with classifier '"
                    + artifactClassifier + "' expected at location \"" + expectedLocation + "\", but is missing");
        } else if (!(expectedLocation.equals(actualLocation.getAbsoluteFile()))) {
            throw new MojoExecutionException("Unexpected build result of " + project + ": Artifact with classifier '"
                    + artifactClassifier + "' expected at location \"" + expectedLocation + "\", but is at \""
                    + actualLocation.getAbsolutePath() + "\"");
        }
    }

    private static void verifyFilePresenceInTargetFolder(ReactorProject project, String expectedPathInTarget)
            throws MojoExecutionException {
        File expectedLocation = project.getBuildDirectory().getChild(expectedPathInTarget);
        if (!expectedLocation.isFile()) {
            throw new MojoExecutionException("Unexpected build result of " + project + ": File \"" + expectedLocation
                    + "\" is missing");
        }
    }

}
