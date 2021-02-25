/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.project.BuildOutputJar;
import org.eclipse.tycho.core.osgitools.project.EclipsePluginProject;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.packaging.sourceref.SourceReferenceComputer;
import org.eclipse.tycho.packaging.sourceref.SourceReferencesProvider;

/**
 * Creates a jar-based plugin and attaches it as an artifact
 */
@Mojo(name = "package-plugin", threadSafe = true)
public class PackagePluginMojo extends AbstractTychoPackagingMojo {

    /**
     * The output directory of the jar file
     * 
     * By default this is the Maven "target/" directory.
     */
    @Parameter(property = "project.build.directory", required = true)
    protected File buildDirectory;

    protected EclipsePluginProject pdeProject;

    /**
     * The Jar archiver.
     */
    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    /**
     * Additional files to be included in the bundle jar. This can be used when
     * <tt>bin.includes</tt> in build.properties is not flexible enough , e.g. for generated files.
     * If conflicting, additional files win over <tt>bin.includes</tt><br/>
     * Example:<br/>
     * 
     * <pre>
     * &lt;additionalFileSets&gt;
     *  &lt;fileSet&gt;
     *   &lt;directory&gt;${project.build.directory}/mytool-gen/&lt;/directory&gt;
     *   &lt;includes&gt;
     *    &lt;include&gt;&#42;&#42;/*&lt;/include&gt;
     *   &lt;/includes&gt;
     *  &lt;/fileSet&gt;     
     * &lt;/additionalFileSets&gt;
     * </pre>
     * 
     */
    @Parameter
    private DefaultFileSet[] additionalFileSets;

    /**
     * Name of the generated JAR.
     */
    @Parameter(property = "project.build.finalName", alias = "jarName", required = true)
    protected String finalName;

    /**
     * The <a href="http://maven.apache.org/shared/maven-archiver/">maven archiver</a> to use. One
     * of the archiver properties is the <code>addMavenDescriptor</code> flag, which indicates
     * whether the generated archive will contain the pom.xml and pom.properties file. If no archive
     * configuration is specified, the default value is <code>true</code>. If the maven descriptor
     * should not be added to the artifact, use the following configuration:
     * 
     * <pre>
     * &lt;plugin&gt;
     *   &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *   &lt;artifactId&gt;tycho-packaging-plugin&lt;/artifactId&gt;
     *   &lt;version&gt;${tycho-version}&lt;/version&gt;
     *   &lt;configuration&gt;
     *     &lt;archive&gt;
     *       &lt;addMavenDescriptor&gt;false&lt;/addMavenDescriptor&gt;
     *     &lt;/archive&gt;
     *   &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Whether to generate an
     * <a href="https://wiki.eclipse.org/PDE/UI/SourceReferences">Eclipse-SourceReferences</a>
     * MANIFEST header. When using this parameter, property ${tycho.scmUrl} must be set and be a
     * valid <a href="http://maven.apache.org/scm/scm-url-format.html">maven SCM URL</a>.
     * 
     * Example configuration:
     * 
     * <pre>
     *         &lt;sourceReferences&gt;
     *           &lt;generate&gt;true&lt;/generate&gt;
     *         &lt;/sourceReferences&gt;
     * </pre>
     * 
     * Note that a {@link SourceReferencesProvider} component must be registered for the SCM type
     * being used. You may also override the generated value by configuring:
     * 
     * <pre>
     *         &lt;sourceReferences&gt;
     *           &lt;generate&gt;true&lt;/generate&gt;
     *           &lt;customValue&gt;scm:myscm:customSourceReferenceValue&lt;/customValue&gt;
     *         &lt;/sourceReferences&gt;
     * </pre>
     */
    @Parameter
    private SourceReferences sourceReferences = new SourceReferences();

    @Component
    private SourceReferenceComputer soureReferenceComputer;

    @Override
    public void execute() throws MojoExecutionException {
        pdeProject = (EclipsePluginProject) DefaultReactorProject.adapt(project)
                .getContextValue(TychoConstants.CTX_ECLIPSE_PLUGIN_PROJECT);

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
        BuildProperties buildProperties = pdeProject.getBuildProperties();
        String customManifest = buildProperties.getJarToManifestMap().get(jarName);
        try {
            File jarFile = new File(project.getBasedir(), jarName);
            JarArchiver archiver = new JarArchiver();
            archiver.setDestFile(jarFile);
            archiver.addDirectory(jar.getOutputDirectory());
            if (customManifest != null) {
                for (File sourceFolder : jar.getSourceFolders()) {
                    File manifestFile = new File(sourceFolder, customManifest);
                    if (manifestFile.isFile()) {
                        archiver.setManifest(manifestFile);
                        break;
                    }
                }
            }
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
            // 1. additional filesets should win over bin.includes, so we add them first
            if (additionalFileSets != null) {
                for (DefaultFileSet fileSet : additionalFileSets) {
                    if (fileSet.getDirectory() != null && fileSet.getDirectory().isDirectory()) {
                        archiver.getArchiver().addFileSet(fileSet);
                    }
                }
            }
            List<String> binIncludesIgnoredForValidation = new ArrayList<>();
            // 2. handle dir classpath entries and "."
            for (BuildOutputJar outputJar : pdeProject.getOutputJarMap().values()) {
                String jarName = outputJar.getName();
                if (binIncludesList.contains(jarName) && outputJar.isDirClasspathEntry()) {
                    binIncludesIgnoredForValidation.add(jarName);
                    String prefix = ".".equals(jarName) ? "" : jarName;
                    archiver.getArchiver().addDirectory(outputJar.getOutputDirectory(), prefix);
                }
            }
            // 3. handle nested jars and included resources
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
            archiver.createArchive(session, project, archive);
            return pluginFile;
        } catch (IOException | ArchiverException | ManifestException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error assembling JAR", e);
        }
    }

    private File updateManifest() throws IOException, MojoExecutionException {
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
        soureReferenceComputer.addSourceReferenceHeader(mf, sourceReferences, project);
        mfile = new File(project.getBuild().getDirectory(), "MANIFEST.MF");
        mfile.getParentFile().mkdirs();
        try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(mfile))) {
            mf.write(os);
        }

        return mfile;
    }

}
