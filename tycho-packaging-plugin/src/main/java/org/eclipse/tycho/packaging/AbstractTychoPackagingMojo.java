/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.TychoProperties;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

public abstract class AbstractTychoPackagingMojo extends AbstractMojo {

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
	@Parameter(property = TychoProperties.BUILD_QUALIFIER)
    protected String qualifier;

    /**
     * If set to <code>true</code> (the default), missing build.properties bin.includes will cause
     * build failure. If set to <code>false</code>, missing build.properties bin.includes will be
     * reported as warnings but the build will not fail.
     */
    @Parameter(defaultValue = "true")
    protected boolean strictBinIncludes;

    /**
     * Additional files to be included in the final <code>.jar</code>.
     * <p>
     * A typical usage might be when <code>bin.includes</code> in <code>build.properties</code>
     * is not flexible enough, e.g., for generated files, as when conflicting additional files
     * win over <code>bin.includes</code>.
     * <p>
     * Example:
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
     * Note: currently, additional file sets are not used for the <code>package-iu</code> goal.
     */
    @Parameter
    protected DefaultFileSet[] additionalFileSets;

    /** If {@code true}, skips the packaging entirely. */
    @Parameter(defaultValue = "false")
    protected boolean skip;

    @Inject
    protected PlexusContainer plexus;

    @Inject
    protected MavenProjectHelper projectHelper;

    @Inject
    private Map<String, TychoProject> projectTypes;

    @Inject
    private IncludeValidationHelper includeValidationHelper;

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
			// keep ignoring the following files after
			// https://github.com/codehaus-plexus/plexus-utils/pull/174
			allExcludes.add("**/.gitignore");
			allExcludes.add("**/.gitattributes");
        }

        fileSet.setExcludes(allExcludes.toArray(new String[allExcludes.size()]));

        return fileSet;
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

}
