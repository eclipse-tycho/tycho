/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.IU;

/**
 * Creates the zip for the IU and attaches it as an artifact
 */
@Mojo(name = "package-iu", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class PackageIUMojo extends AbstractTychoPackagingMojo {
    private static final Object LOCK = new Object();

    @Parameter(property = "project.build.directory", required = true, readonly = true)
    protected File outputDirectory;

    /**
     * Folder containing the files to include in the final zip.
     */
    @Parameter(property = "project.build.outputDirectory")
    private String artifactContentFolder;

    @Parameter(property = "project.basedir", required = true, readonly = true)
    private File basedir;

    @Component
    private IUXmlTransformer iuTransformer;

    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            outputDirectory.mkdirs();

            IU iu = IU.loadIU(basedir);
            File iuXML = new File(outputDirectory, IU.SOURCE_FILE_NAME);
            try {
                addSelfCapability(iu);
                addArtifactReference(iu);
                addMavenProperties(iu);
                expandVersions(iu);
                IU.write(iu, iuXML);
            } catch (IOException e) {
                throw new MojoExecutionException("Error updating " + IU.SOURCE_FILE_NAME, e);
            }

            //Create the artifact
            File artifactForIU = createArtifact();
            project.getArtifact().setFile(artifactForIU);
        }
    }

    private void addMavenProperties(IU iu) {
        iuTransformer.injectMavenProperties(iu, project);
    }

    private void addSelfCapability(IU iu) {
        iuTransformer.addSelfCapability(iu);
    }

    private void addArtifactReference(IU iu) {
        if (!hasPayload())
            return;
        if (hasArtifactReference(iu))
            return;
        iu.addArtifact("binary", iu.getId(), iu.getVersion());
    }

    private void expandVersions(IU iu) throws MojoFailureException {
        iuTransformer.replaceSelfQualifiers(iu, DefaultReactorProject.adapt(project).getExpandedVersion(),
                DefaultReactorProject.adapt(project).getBuildQualifier());
        iuTransformer.replaceQualifierInCapabilities(iu.getProvidedCapabilites(),
                DefaultReactorProject.adapt(project).getBuildQualifier());

        TargetPlatform targetPlatform = TychoProjectUtils
                .getTargetPlatformIfAvailable(DefaultReactorProject.adapt(project));
        if (targetPlatform == null) {
            getLog().warn(
                    "Skipping version reference expansion in p2iu project using the deprecated -Dtycho.targetPlatform configuration");
            return;
        }
        iuTransformer.replaceZerosInRequirements(iu, targetPlatform);
        iuTransformer.replaceQualifierInRequirements(iu, targetPlatform);
    }

    private boolean hasPayload() {
        return getPayloadDir().isDirectory();
    }

    private File createArtifact() throws MojoExecutionException {
        try {
            File payload = getPayloadDir();
            File newArtifact = new File(outputDirectory, project.getArtifactId() + "-" + project.getVersion() + ".zip");
            if (newArtifact.exists()) {
                newArtifact.delete();
            }

            if (hasPayload()) {
                DefaultFileSet fs = new DefaultFileSet();
                fs.setDirectory(payload);
                zipArchiver.addFileSet(fs);
                zipArchiver.setDestFile(newArtifact);
                zipArchiver.setCompress(true);
                zipArchiver.createArchive();
            } else {
                //Force create the file
                newArtifact.createNewFile();
            }
            return newArtifact;
        } catch (IOException | ArchiverException e) {
            throw new MojoExecutionException("Error assembling ZIP", e);
        }
    }

    private File getPayloadDir() {
        return new File(artifactContentFolder);
    }

    private boolean hasArtifactReference(IU iu) {
        return iu.getSelfArtifact() != null;
    }
}
