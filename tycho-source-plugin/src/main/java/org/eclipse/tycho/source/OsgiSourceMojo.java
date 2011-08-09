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
package org.eclipse.tycho.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.osgi.framework.Version;

/**
 * Goal to create a JAR-package containing all the source files of a osgi project.
 * 
 * @extendsPlugin source
 * @extendsGoal jar
 * @goal plugin-source
 * @phase prepare-package
 */
public class OsgiSourceMojo extends AbstractSourceJarMojo {

    private static final String MANIFEST_HEADER_BUNDLE_MANIFEST_VERSION = "Bundle-ManifestVersion";
    private static final String MANIFEST_HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String MANIFEST_HEADER_BUNDLE_VERSION = "Bundle-Version";
    private static final String MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle";
    private static final String VERSION_QUALIFIER = "qualifier";

    /**
     * If set to true, compiler will use source folders defined in build.properties file and will
     * ignore ${project.compileSourceRoots}/${project.testCompileSourceRoots}.
     * 
     * Compilation will fail with an error, if this parameter is set to true but the project does
     * not have valid build.properties file.
     * 
     * @parameter default-value="true"
     */
    private boolean usePdeSourceRoots;

    /**
     * Whether the source jar should be an Eclipse source bundle.
     * 
     * @parameter default-value="true"
     */
    private boolean sourceBundle;

    /**
     * The suffix to be added to the symbolic name of the bundle to construct the symbolic name of
     * the Eclipse source bundle.
     * 
     * @parameter expression="${sourceBundleSuffix}" default-value=".source"
     */
    private String sourceBundleSuffix;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal. Only
     * used when creating a source bundle.
     * 
     * @parameter expression="${buildQualifier}"
     */
    private String qualifier;

    /**
     * Whether default source excludes for SCM files defined in {@see
     * AbstractScanner#DEFAULTEXCLUDES} should be used.
     * 
     * @parameter default-value="true"
     */
    protected boolean useDefaultSourceExcludes;

    /**
     * Whether source folders are required or not. If not required (the default), projects without
     * source folders will be silently ignored.
     * 
     * @parameter default-value="false"
     * @readonly
     */
    protected boolean requireSourceRoots;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /** {@inheritDoc} */
    protected List<String> getSources(MavenProject p) throws MojoExecutionException {
        if (usePdeSourceRoots) {
            Properties props = getBuildProperties();
            List<String> sources = new ArrayList<String>();
            for (Entry<Object, Object> entry : props.entrySet()) {
                if (((String) entry.getKey()).startsWith("source.")) {
                    sources.addAll(getSourceDirs((String) entry.getValue()));
                }
            }
            if (requireSourceRoots && sources.isEmpty()) {
                throw new MojoExecutionException("no source folders found in build.properties");
            }
            return sources;
        } else {
            return p.getCompileSourceRoots();
        }
    }

    private List<String> getSourceDirs(String sourceRaw) {
        List<String> sources = new ArrayList<String>();
        for (String source : sourceRaw.split(",")) {
            sources.add(new File(project.getBasedir(), source.trim()).getAbsolutePath());
        }
        return sources;
    }

    /** {@inheritDoc} */
    protected List<Resource> getResources(MavenProject p) throws MojoExecutionException {
        if (excludeResources) {
            return Collections.emptyList();
        }
        if (usePdeSourceRoots) {
            Properties props = getBuildProperties();
            String srcIncludes = props.getProperty("src.includes");
            if (srcIncludes == null) {
                return Collections.emptyList();
            }
            List<String> srcInludesList = toFilePattern(props.getProperty("src.includes"));
            List<String> srcExcludesList = toFilePattern(props.getProperty("src.excludes"));
            //FileSet src = getFileSet(project.getBasedir(), includes, excludes);
            Resource resource = new Resource();
            resource.setDirectory(project.getBasedir().getAbsolutePath());
            resource.setExcludes(srcExcludesList);
            resource.setIncludes(srcInludesList);
            return Collections.singletonList(resource);
        }

        return p.getResources();
    }

    protected List<String> toFilePattern(String pattern) {
        ArrayList<String> result = new ArrayList<String>();
        if (pattern != null) {
            StringTokenizer st = new StringTokenizer(pattern, ",");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }

        return result;
    }

    protected FileSet getFileSet(File basedir, List<String> includes, List<String> excludes) {
        DefaultFileSet fileSet = new DefaultFileSet();
        fileSet.setDirectory(basedir);
        fileSet.setIncludes(includes.toArray(new String[includes.size()]));

        Set<String> allExcludes = new LinkedHashSet<String>();
        if (excludes != null) {
            allExcludes.addAll(excludes);
        }
        if (useDefaultSourceExcludes) {
            allExcludes.addAll(Arrays.asList(AbstractScanner.DEFAULTEXCLUDES));
        }

        fileSet.setExcludes(allExcludes.toArray(new String[allExcludes.size()]));

        return fileSet;
    }

    /** {@inheritDoc} */
    protected String getClassifier() {
        return ReactorProject.SOURCE_ARTIFACT_CLASSIFIER;
    }

    // TODO check how to fix this code duplicated
    private Properties getBuildProperties() throws MojoExecutionException {
        File file = new File(project.getBasedir(), "build.properties");
        if (!file.canRead()) {
            throw new MojoExecutionException("Unable to read build.properties file");
        }

        Properties buildProperties = new Properties();
        try {
            InputStream is = new FileInputStream(file);
            try {
                buildProperties.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception reading build.properties file", e);
        }
        return buildProperties;
    }

    @Override
    protected void updateSourceManifest(MavenArchiveConfiguration mavenArchiveConfiguration) {
        super.updateSourceManifest(mavenArchiveConfiguration);

        if (sourceBundle) {
            addSourceBundleManifestEntries(mavenArchiveConfiguration);
        }
    }

    private void addSourceBundleManifestEntries(MavenArchiveConfiguration mavenArchiveConfiguration) {
        TychoProject projectType = projectTypes.get(project.getPackaging());
        ArtifactKey artifactKey = projectType.getArtifactKey(DefaultReactorProject.adapt(project));
        String symbolicName = artifactKey.getId();
        String version = artifactKey.getVersion();

        if (symbolicName != null && version != null) {
            mavenArchiveConfiguration.addManifestEntry(MANIFEST_HEADER_BUNDLE_MANIFEST_VERSION, "2");

            mavenArchiveConfiguration.addManifestEntry(MANIFEST_HEADER_BUNDLE_SYMBOLIC_NAME, symbolicName
                    + sourceBundleSuffix);

            Version expandedVersion = getExpandedVersion(version);

            mavenArchiveConfiguration.addManifestEntry(MANIFEST_HEADER_BUNDLE_VERSION, expandedVersion.toString());

            mavenArchiveConfiguration.addManifestEntry(MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE, symbolicName
                    + ";version=\"" + expandedVersion + "\";roots:=\".\"");
        } else {
            getLog().info("NOT adding source bundle manifest entries. Incomplete or no bundle information available.");
        }
    }

    private Version getExpandedVersion(String versionStr) {
        Version version = Version.parseVersion(versionStr);
        if (VERSION_QUALIFIER.equals(version.getQualifier())) {
            return new Version(version.getMajor(), version.getMinor(), version.getMicro(), qualifier);
        }
        return version;
    }

    @Override
    protected boolean isRelevantProject(MavenProject project) {
        String packaging = project.getPackaging();
        boolean relevant = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
        if (!relevant) {
            return false;
        }
        try {
            return requireSourceRoots || !getSources(project).isEmpty();
        } catch (MojoExecutionException e) {
            // can't happen
        }
        return true;
    }

}
