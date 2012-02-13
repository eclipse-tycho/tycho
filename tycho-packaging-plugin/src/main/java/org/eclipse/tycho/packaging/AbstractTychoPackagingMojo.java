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

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.ArtifactDependencyWalker;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;

/**
 * @requiresProject
 */
public abstract class AbstractTychoPackagingMojo extends AbstractMojo {
    /**
     * @parameter expression="${session}"
     * @readonly
     */
    protected MavenSession session;

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean useDefaultExcludes;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal.
     * 
     * @parameter expression="${buildQualifier}"
     */
    protected String qualifier;

    /**
     * If set to <code>true</code> (the default), missing build.properties bin.includes will cause
     * build failure. If set to <code>false</code>, missing build.properties bin.includes will be
     * reported as warnings but the build will not fail.
     * 
     * @parameter default-value="true"
     */
    protected boolean strictBinIncludes;

    /** @component */
    protected PlexusContainer plexus;

    /** @component */
    protected MavenProjectHelper projectHelper;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /**
     * @component
     */
    private IncludeValidationHelper includeValidationHelper;

    protected FileSet getFileSet(File basedir, List<String> includes, List<String> excludes) {
        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory(basedir);
        fileSet.setIncludes(includes.toArray(new String[includes.size()]));

        Set<String> allExcludes = new LinkedHashSet<String>();
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
        return getTychoProjectFacet().getDependencyWalker(project);
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
        return getTychoProjectFacet().getDependencyArtifacts(project);
    }

    protected void expandVersion() {
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        String originalVersion = getTychoProjectFacet().getArtifactKey(reactorProject).getVersion();
        reactorProject.setExpandedVersion(originalVersion, qualifier);
    }

    protected void checkBinIncludesExist(BuildProperties buildProperties, String... ignoredIncludes)
            throws MojoExecutionException {
        includeValidationHelper.checkBinIncludesExist(project, buildProperties, strictBinIncludes, ignoredIncludes);
    }

}
