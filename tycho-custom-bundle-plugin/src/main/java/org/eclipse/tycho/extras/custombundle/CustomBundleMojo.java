/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.custombundle;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * Builds OSGi bundle
 * 
 * @goal custom-bundle
 */
public class CustomBundleMojo extends AbstractMojo {

    /**
     * Location of OSGi bundle, must have META-INF/MANIFEST.MF bundle manifest file.
     * 
     * @parameter
     * @required
     */
    private File bundleLocation;

    /**
     * Classifier of attached artifact.
     * 
     * @parameter
     * @required
     */
    private String classifier;

    /**
     * File patterns to include from bundleLocation. Include everything by default.
     * 
     * @parameter
     */
    private String[] includes = new String[] { "**/*.*" };

    /**
     * File patterns to exclude from bundleLocation.
     * 
     * @parameter
     */
    private String[] excludes;

    /**
     * Additional files to be included in the generated bundle.
     * 
     * @parameter
     * @required
     */
    private List<DefaultFileSet> fileSets;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File outputJarFile = getOutputJarFile();

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJarFile);

        try {
            archiver.getArchiver().setManifest(updateManifest());

            DefaultFileSet mainFileSet = new DefaultFileSet();
            mainFileSet.setDirectory(bundleLocation);
            mainFileSet.setIncludes(includes);
            mainFileSet.setExcludes(excludes);

            archiver.getArchiver().addFileSet(mainFileSet);

            for (FileSet fileSet : fileSets) {
                archiver.getArchiver().addFileSet(fileSet);
            }

            archiver.createArchive(project, archive);

            projectHelper.attachArtifact(project, outputJarFile, classifier);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not create OSGi bundle", e);
        }
    }

    protected File getOutputJarFile() {
        String filename = project.getArtifactId() + "-" + project.getVersion() + "-" + classifier + ".jar";
        return new File(project.getBuild().getDirectory(), filename);
    }

    // copy&paste from PackagePluginMojo
    private File updateManifest() throws FileNotFoundException, IOException, MojoExecutionException {
        File mfile = new File(bundleLocation, "META-INF/MANIFEST.MF");

        InputStream is = new FileInputStream(mfile);
        Manifest mf;
        try {
            mf = new Manifest(is);
        } finally {
            is.close();
        }
        Attributes attributes = mf.getMainAttributes();

        if (attributes.getValue(Name.MANIFEST_VERSION) == null) {
            attributes.put(Name.MANIFEST_VERSION, "1.0");
        }

        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        attributes.putValue("Bundle-Version", reactorProject.getExpandedVersion());

        mfile = new File(project.getBuild().getDirectory(), classifier + "-MANIFEST.MF");
        mfile.getParentFile().mkdirs();
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile));
        try {
            mf.write(os);
        } finally {
            os.close();
        }

        return mfile;
    }

}
