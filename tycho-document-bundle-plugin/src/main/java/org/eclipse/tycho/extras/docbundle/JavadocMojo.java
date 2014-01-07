/*******************************************************************************
 * Copyright (c) 2013 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.core.osgitools.BundleReader;

/**
 * Create the javadoc based API reference for this bundle <br/>
 * This mojo creates the javadoc documentation by calling the javadoc application from the command
 * line. In addition it creates a ready to include toc-xml file for the Eclipse Help system. <br/>
 * The sources for creating the javadoc are generated automatically based on the dependency that
 * this project has. As dependency you can specify any other maven project, for example the feature
 * project that references you other bundles. Included features will be added to the list.
 * 
 * @goal javadoc
 * @phase process-classes
 * @requiresDependencyResolution compile+runtime
 * @threadSafe false
 * @since 0.20.0
 * 
 */
public class JavadocMojo extends AbstractMojo {
    /**
     * The directory where the javadoc content will be generated
     * 
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/reference/api"
     * @required
     */
    private File outputDirectory;

    /**
     * The base output directory
     * 
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * The build directory where temporary build files will be placed
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File buildDirectory;

    /**
     * An option to clean out the whole outputDirectory first.
     * 
     * @parameter expression="${cleanFirst}" default-value="true"
     */
    private boolean cleanFirst;

    /**
     * @component
     */
    private ToolchainManager toolchainManager;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List<MavenProject> reactorProjects;

    /**
     * The scopes that the dependencies must have in order to be included
     * 
     * @parameter default-value="compile,provided" expression="${scopes}"
     */
    private Set<String> scopes = new HashSet<String>();

    /**
     * Maven module types that will be used to include the source
     * 
     * @parameter default-value="eclipse-plugin" expression="${sourceTypes}"
     */
    private Set<String> sourceTypes = new HashSet<String>();

    /**
     * Options for calling the javadoc application
     * 
     * @parameter expression="${javadocOptions}"
     */
    private JavadocOptions javadocOptions = new JavadocOptions();

    /**
     * Options for creating the toc files.
     * 
     * @parameter expression="${tocOptions}"
     */
    private TocOptions tocOptions = new TocOptions();

    /**
     * The output location of the toc file.<br/>
     * This file will be overwritten.
     * 
     * @parameter default-value="${project.build.directory}/tocjavadoc.xml" expression="${tocFile}"
     */
    private File tocFile;

    /**
     * @component
     */
    private BundleReader bundleReader;

    public void setTocOptions(TocOptions tocOptions) {
        this.tocOptions = tocOptions;
    }

    public void setSourceTypes(final Set<String> sourceTypes) {
        this.sourceTypes = sourceTypes;
    }

    public void setScopes(final Set<String> scopes) {
        this.scopes = scopes;
    }

    public void setJavadocOptions(final JavadocOptions javadocOptions) {
        this.javadocOptions = javadocOptions;
    }

    public void execute() throws MojoExecutionException {
        getLog().info("Scopes: " + this.scopes);
        getLog().info("Output directory: " + this.outputDirectory);
        getLog().info("BaseDir: " + this.basedir);

        if (this.cleanFirst) {
            getLog().info("Cleaning up first");
            cleanUp();
        }

        final JavadocRunner runner = new JavadocRunner();
        runner.setLog(getLog());
        runner.setOutput(this.outputDirectory);
        runner.setBuildDirectory(this.buildDirectory);
        runner.setToolchainManager(this.toolchainManager);
        runner.setSession(this.session);

        final GatherSourcesVisitor gsv = new GatherSourcesVisitor();
        visitProjects(this.session.getCurrentProject().getDependencies(), this.scopes, gsv);

        getLog().info(String.format("%s source folders", gsv.getSourceFolders().size()));
        for (final File file : gsv.getSourceFolders()) {
            getLog().info("Source folder: " + file);
        }

        // TODO: use tycho's approach of gathering the classpath
        final GatherClasspathVisitor gcv = new GatherClasspathVisitor();
        visitProjects(this.session.getCurrentProject().getDependencies(), this.scopes, gcv);

        final Collection<String> cp = gcv.getClassPath();

        getLog().info(String.format("%s classpath deps", cp.size()));
        for (final String ele : cp) {
            getLog().info("CP: " + ele);
        }

        runner.setBundleReader(this.bundleReader);
        runner.setOptions(this.javadocOptions);
        runner.setSourceFolders(gsv.getSourceFolders());
        runner.setClassPath(cp);

        // Setup toc writer

        final TocWriter tocWriter = new TocWriter();
        tocWriter.setOptions(this.tocOptions);
        tocWriter.setJavadocDir(this.outputDirectory);
        tocWriter.setBasedir(this.basedir);
        tocWriter.setLog(getLog());

        try {
            runner.run();
            tocWriter.writeTo(this.tocFile);
        } catch (final Exception e) {
            throw new MojoExecutionException("Failed to run javadoc", e);
        }
    }

    private void cleanUp() throws MojoExecutionException {
        if (!this.outputDirectory.exists()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(this.outputDirectory);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to clean output directory", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void visitProjects(final List<?> dependencies, final Set<String> scopes, final ProjectVisitor visitor)
            throws MojoExecutionException {
        for (final Dependency dep : (List<Dependency>) dependencies) {
            getLog().debug("Dependency: " + dep + " / scope=" + dep.getScope());

            final String scope = dep.getScope();

            if (scopes.contains(scope)) {
                visitDeps(dep, visitor, scopes);
            }
        }
    }

    private interface ProjectVisitor {
        public void visit(MavenProject project) throws MojoExecutionException;
    }

    private class GatherSourcesVisitor implements ProjectVisitor {
        private final Set<File> sourceFolders = new HashSet<File>();

        public void visit(final MavenProject project) {
            if (JavadocMojo.this.sourceTypes.contains(project.getPackaging())) {
                for (final String root : (Collection<String>) project.getCompileSourceRoots()) {
                    getLog().debug("\tAdding source root: " + root);
                    final File rootFile = new File(root);
                    if (rootFile.isDirectory()) {
                        this.sourceFolders.add(rootFile);
                    }
                }
            }
        }

        public Set<File> getSourceFolders() {
            return this.sourceFolders;
        }
    }

    private class GatherClasspathVisitor implements ProjectVisitor {
        private final Set<String> classPath = new HashSet<String>();

        public void visit(final MavenProject project) throws MojoExecutionException {
            for (final Dependency dep : (List<Dependency>) project.getDependencies()) {
                if (dep.getSystemPath() != null) {
                    this.classPath.add(dep.getSystemPath());
                }
            }
        }

        public Set<String> getClassPath() {
            return this.classPath;
        }
    }

    private void visitDeps(final Dependency dep, final ProjectVisitor visitor, final Set<String> scopes)
            throws MojoExecutionException {
        final MavenProject project = findProject(dep.getGroupId(), dep.getArtifactId());
        if (project == null) {
            getLog().info(String.format("Did not find project %s in reactor", dep));
            return;
        }

        getLog().debug("Adding sources from: " + project);

        visitor.visit(project);

        getLog().debug("Scanning dependencies: " + project.getDependencies().size());
        visitProjects(project.getDependencies(), scopes, visitor);

        getLog().debug("Done processing: " + project);
    }

    private MavenProject findProject(final String groupId, final String artifactId) {
        getLog().debug(String.format("findProject - groupId: %s, artifactId: %s", groupId, artifactId));

        for (final MavenProject p : this.reactorProjects) {
            if (!p.getGroupId().equals(groupId)) {
                continue;
            }
            if (!p.getArtifactId().equals(artifactId)) {
                continue;
            }
            return p;
        }
        return null;
    }
}
