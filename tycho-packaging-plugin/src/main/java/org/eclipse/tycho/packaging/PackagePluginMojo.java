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
package org.eclipse.tycho.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 * 
 * @goal package-plugin
 */
public class PackagePluginMojo extends AbstractTychoPackagingMojo {

    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    protected File buildDirectory;

    protected EclipsePluginProject pdeProject;

    /**
     * The Jar archiver.
     * 
     * parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}" required
     */
    private JarArchiver jarArchiver = new JarArchiver();

    /**
     * Name of the generated JAR.
     * 
     * @parameter alias="jarName" expression="${project.build.finalName}"
     * @required
     */
    protected String finalName;

    /**
     * The maven archiver to use.
     * 
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    public void execute() throws MojoExecutionException {
        pdeProject = (EclipsePluginProject) project.getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT);

        expandVersion();

        createSubJars();

        File pluginFile = createPluginJar();

        project.getArtifact().setFile(pluginFile);
    }

    private void createSubJars() throws MojoExecutionException {
        for (BuildOutputJar jar : pdeProject.getOutputJars()) {
            if (!jar.isDirClasspathEntry()) {
                makeJar(jar);
            }
        }
    }

    private File makeJar(BuildOutputJar jar) throws MojoExecutionException {
        String jarName = jar.getName();
        try {
            File jarFile = new File(project.getBasedir(), jarName);
            JarArchiver archiver = new JarArchiver();
            archiver.setDestFile(jarFile);
            archiver.addDirectory(jar.getOutputDirectory());
            archiver.createArchive();
            return jarFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not create jar " + jarName, e);
        }
    }

    private File createPluginJar() throws MojoExecutionException {
        try {
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(jarArchiver);

            File pluginFile = new File(buildDirectory, finalName + ".jar");
            if (pluginFile.exists()) {
                pluginFile.delete();
            }
            BuildProperties buildProperties = pdeProject.getBuildProperties();
            List<String> binIncludesList = buildProperties.getBinIncludes();
            List<String> binExcludesList = buildProperties.getBinExcludes();

            List<String> binIncludesIgnoredForValidation = new ArrayList<String>();
            // 1. handle dir classpath entries and "."            
            for (BuildOutputJar outputJar : pdeProject.getOutputJarMap().values()) {
                String jarName = outputJar.getName();
                if (binIncludesList.contains(jarName) && outputJar.isDirClasspathEntry()) {
                    binIncludesIgnoredForValidation.add(jarName);
                    String prefix = ".".equals(jarName) ? "" : jarName;
                    archiver.getArchiver().addDirectory(outputJar.getOutputDirectory(), prefix);
                }
            }
            // 2. handle nested jars and included resources
            checkBinIncludesExist(buildProperties, binIncludesIgnoredForValidation.toArray(new String[0]));
            archiver.getArchiver().addFileSet(getFileSet(project.getBasedir(), binIncludesList, binExcludesList));

            File manifest = updateManifest();
            if (manifest.exists()) {
                archive.setManifestFile(manifest);
            }

            archiver.setOutputFile(pluginFile);
            if (!archive.isForced()) {
                // optimized archive creation not supported for now because of build qualifier mismatch issues
                // see TYCHO-502
                getLog().warn("ignoring unsupported archive forced = false parameter.");
                archive.setForced(true);
            }
            archiver.createArchive(project, archive);

            return pluginFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }
    }

    private File updateManifest() throws FileNotFoundException, IOException, MojoExecutionException {
        File mfile = new File(project.getBasedir(), "META-INF/MANIFEST.MF");

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

        mfile = new File(project.getBuild().getDirectory(), "MANIFEST.MF");
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
