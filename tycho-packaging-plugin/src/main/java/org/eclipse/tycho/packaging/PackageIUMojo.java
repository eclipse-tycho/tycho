/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

@Mojo(name = "package-iu", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageIUMojo extends AbstractTychoPackagingMojo {

    /**
     * The output directory of the jar file
     * 
     * By default this is the Maven "target/" directory.
     */
    @Parameter(property = "project.build.directory", required = true)
    protected File outputDirectory;

    @Parameter(property = "project.basedir")
    private File basedir;

    @Component
    private IUXmlTransformer iuTransformer;
    /**
     * The Jar archiver.
     */
    @Component(role = Archiver.class, hint = "zip")
    private ZipArchiver zipArchiver;

    @Parameter(defaultValue = "payload")
    private String artifactContentFolder;

    public void execute() throws MojoExecutionException, MojoFailureException {
        outputDirectory.mkdirs();

        //Munge the metadata to reflect this
        IU iu = IU.loadIU(basedir);
        File iuXML = new File(outputDirectory, IU.P2_IU);
        try {
            addArtifactReference(iu);
            expandVersions(iu);
            IU.write(iu, iuXML);
        } catch (IOException e) {
            throw new MojoExecutionException("Error updating " + IU.P2_IU, e);
        }

        //Create the artifact
        File artifactForIU = createArtifact();
        project.getArtifact().setFile(artifactForIU);

    }

    private void addArtifactReference(IU iu) {
        if (!hasPayload())
            return;
        if (hasArtifactReference(iu))
            return;
        iuTransformer.addArtifact(iu, "binary", iu.getId(), iu.getVersion());
    }

    private void expandVersions(IU iu) throws MojoFailureException {
        iuTransformer.replaceSelfQualifiers(iu, DefaultReactorProject.adapt(project).getExpandedVersion());
        iuTransformer.replaceQualifierInCapabilities(iu, DefaultReactorProject.adapt(project).getBuildQualifier());
        iuTransformer.injectMavenProperties(iu, project);

        TargetPlatform targetPlatform = TychoProjectUtils.getTargetPlatformIfAvailable(project);
        if (targetPlatform == null) {
            getLog().warn(
                    "Skipping version reference expansion in p2iu project using the deprecated -Dtycho.targetPlatform configuration");
            return;
        }
        iuTransformer.replaceZerosInRequirements(iu, targetPlatform);
    }

    private boolean hasPayload() {
        return getPayload().exists();
    }

    private File createArtifact() throws MojoExecutionException {
        try {
            File payload = getPayload();
            File newArtifact = new File(outputDirectory, project.getArtifactId() + "-" + project.getVersion() + ".zip"); //TODO give a real name
            if (newArtifact.exists()) {
                newArtifact.delete();
            }

            if (payload.exists()) {
                DefaultFileSet fs = new DefaultFileSet();
                fs.setDirectory(payload);
                zipArchiver.addFileSet(fs);
            } else {
                zipArchiver.addFile(new File(outputDirectory, IU.P2_IU), "emptyArtifact.xml");
            }
            zipArchiver.setDestFile(newArtifact);
            zipArchiver.setCompress(true);
            zipArchiver.createArchive();
            return newArtifact;
        } catch (IOException e) {
            throw new MojoExecutionException("Error assembling ZIP", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error assembling ZIP", e);
        }
    }

    private File getPayload() {
        File payload = new File(project.getBasedir(), artifactContentFolder);
        return payload;
    }

    private boolean hasArtifactReference(IU iu) {
        return iu.getSelfArtifact() != null;
    }
}
