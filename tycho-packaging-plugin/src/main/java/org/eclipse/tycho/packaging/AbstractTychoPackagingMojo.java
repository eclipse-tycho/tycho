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
 *    Christoph Läubrich - Issue #572 - Insert dynamic dependencies into the jar included pom 
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public abstract class AbstractTychoPackagingMojo extends AbstractMojo {

    /**
     * If this property is set (with an arbitrary value) in the current project,
     * then it indicates that the generate-metadata goal has been executed.
     */
    protected static final String GENERATE_METADATA = "tycho.packaging.generateMetadata";

	/**
	 * The output directory of the jar file
	 * 
	 * By default this is the Maven "target/" directory.
	 */
	@Parameter(property = "project.build.directory", required = true)
	protected File buildDirectory;

    @Parameter(property = "session", readonly = true)
    protected MavenSession session;

    @Parameter(property = "project", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "true")
    protected boolean useDefaultExcludes;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     */
    @Parameter(property = "buildQualifier")
    protected String qualifier;

    /**
     * If set to <code>true</code> (the default), missing build.properties bin.includes will cause
     * build failure. If set to <code>false</code>, missing build.properties bin.includes will be
     * reported as warnings but the build will not fail.
     */
    @Parameter(defaultValue = "true")
    protected boolean strictBinIncludes;

    @Component
    protected PlexusContainer plexus;

    @Component
    protected MavenProjectHelper projectHelper;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Component
    private IncludeValidationHelper includeValidationHelper;

	@Component(role = ModelWriter.class)
	protected ModelWriter modelWriter;

	@Component(role = ModelReader.class)
	protected ModelReader modelReader;

    /**
     * @return a {@link FileSet} with the given includes and excludes and the configured default
     *         excludes. An empty list of includes leads to an empty file set.
     */
    protected FileSet getFileSet(File basedir, List<String> includes, List<String> excludes) {
        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory(basedir);

        if (includes.isEmpty()) {
            // FileSet interprets empty list as "everything", so we need to express "nothing" in a different way
            fileSet.setIncludes(new String[] { "" });
        } else {
            fileSet.setIncludes(includes.toArray(new String[includes.size()]));
        }

        Set<String> allExcludes = new LinkedHashSet<>();
        if (excludes != null) {
            allExcludes.addAll(excludes);
        }
        if (useDefaultExcludes) {
            allExcludes.addAll(Arrays.asList(AbstractScanner.DEFAULTEXCLUDES));
        }

        fileSet.setExcludes(allExcludes.toArray(new String[allExcludes.size()]));

        return fileSet;
    }

    protected ArtifactDependencyWalker getDependencyWalker() {
        return getTychoProjectFacet().getDependencyWalker(DefaultReactorProject.adapt(project));
    }

    protected TychoProject getTychoProjectFacet() {
        return getTychoProjectFacet(project.getPackaging());
    }

    protected TychoProject getTychoProjectFacet(String packaging) {
        TychoProject facet = projectTypes.get(packaging);
        if (facet == null) {
            throw new IllegalStateException("Unknown or unsupported packaging type " + packaging);
        }
        return facet;
    }

    protected DependencyArtifacts getDependencyArtifacts() {
        return getTychoProjectFacet().getDependencyArtifacts(DefaultReactorProject.adapt(project));
    }

    protected void checkBinIncludesExist(BuildProperties buildProperties, String... ignoredIncludes)
            throws MojoExecutionException {
        includeValidationHelper.checkBinIncludesExist(project, buildProperties, strictBinIncludes, ignoredIncludes);
    }

	/**
	 * Updates the pom file with the dependencies from the model writing it to the
	 * output directory under [finalName].jar
	 * 
	 * @param finalName
	 * @return the maven project with updated pom location
	 * @throws IOException
	 */
	protected MavenProject updatePom(String finalName) throws IOException {
		getLog().debug("Generate pom descriptor with updated dependencies...");
		Model projectModel = modelReader.read(project.getFile(), null);
		File pomFile;
		if (buildDirectory == null) {
			// this should only happen in unit-tests ...
			pomFile = new File(project.getBasedir(), finalName + ".pom");
		} else {
			pomFile = new File(buildDirectory, finalName + ".pom");
		}
		List<Dependency> dependencies = projectModel.getDependencies();
		dependencies.clear();
		List<Dependency> list = Objects.requireNonNullElse(project.getDependencies(), Collections.emptyList());
		for (Dependency dep : list) {
			Dependency copy = dep.clone();
			copy.setSystemPath(null);
			dependencies.add(copy);
		}
		modelWriter.write(pomFile, null, projectModel);
		MavenProject mavenProject = project.clone(); // don't alter the original project!
		mavenProject.setFile(pomFile);
		return mavenProject;
	}

    /**
     * Checks whether a specific property has been set in the current reactor
     * project. Properties are used e.g. to indicate that the
     * {@code generate-metadata} goal has been executed.
     * 
     * @param key A unique property key. May not be {@code null}.
     * @return {@code true}, in case a property with this key exists.
     */
    protected boolean hasProperty(String key) {
        return project.getProperties().containsKey(key);
    }

}
