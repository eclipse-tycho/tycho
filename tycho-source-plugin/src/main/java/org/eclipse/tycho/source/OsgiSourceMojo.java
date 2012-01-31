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

import static org.eclipse.tycho.packaging.IncludeValidationHelper.checkSourceIncludesExist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
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

    private static final String GOAL = "plugin-source";

    private static final String MANIFEST_HEADER_BUNDLE_MANIFEST_VERSION = "Bundle-ManifestVersion";
    private static final String MANIFEST_HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
    private static final String MANIFEST_HEADER_BUNDLE_VERSION = "Bundle-Version";
    private static final String MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle";
    private static final String VERSION_QUALIFIER = "qualifier";

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

    /**
     * @component
     */
    private BuildPropertiesParser buildPropertiesParser;

    /** {@inheritDoc} */
    protected List<String> getSources(MavenProject p) throws MojoExecutionException {
        return getSources(project, requireSourceRoots, buildPropertiesParser);
    }

    protected static List<String> getSources(MavenProject p, boolean requireSourceRoots,
            BuildPropertiesParser buildPropertiesParser) throws MojoExecutionException {
        List<String> sources = new ArrayList<String>();
        for (List<String> sourceFolderList : buildPropertiesParser.parse(p.getBasedir()).getJarToSourceFolderMap()
                .values()) {
            for (String sourceFolder : sourceFolderList) {
                sources.add(new File(p.getBasedir(), sourceFolder).getAbsolutePath());
            }
        }
        if (requireSourceRoots && sources.isEmpty()) {
            throw new MojoExecutionException("no source folders found in build.properties");
        }
        return sources;
    }

    /** {@inheritDoc} */
    protected List<Resource> getResources(MavenProject p) throws MojoExecutionException {
        if (excludeResources) {
            return Collections.emptyList();
        }
        BuildProperties buildProperties = buildPropertiesParser.parse(p.getBasedir());
        List<String> srcIncludesList = buildProperties.getSourceIncludes();
        if (srcIncludesList.isEmpty()) {
            return Collections.emptyList();
        }
        checkSourceIncludesExist(p, buildProperties);
        Resource resource = new Resource();
        resource.setDirectory(project.getBasedir().getAbsolutePath());
        resource.setExcludes(buildProperties.getSourceExcludes());
        resource.setIncludes(srcIncludesList);
        return Collections.singletonList(resource);
    }

    /** {@inheritDoc} */
    protected String getClassifier() {
        return ReactorProject.SOURCE_ARTIFACT_CLASSIFIER;
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
        return isRelevantProjectImpl(project, buildPropertiesParser);
    }

    protected static boolean isRelevantProjectImpl(MavenProject project, BuildPropertiesParser buildPropertiesParser) {
        String packaging = project.getPackaging();
        boolean relevant = org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
        if (!relevant) {
            return false;
        }

        // this assumes that sources generation has to be explicitly enabled in pom.xml
        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-source-plugin");

        if (plugin == null) {
            return false;
        }

        for (PluginExecution execution : plugin.getExecutions()) {
            if (execution.getGoals().contains(GOAL)) {
                boolean requireSourceRoots = Boolean.parseBoolean(getParameterValue(execution, "requireSourceRoots",
                        "false"));
                if (requireSourceRoots) {
                    return true;
                }
                try {
                    if (!getSources(project, requireSourceRoots, buildPropertiesParser).isEmpty()) {
                        return true;
                    }
                } catch (MojoExecutionException e) {
                    // can't happen because requireSourceRoots==false 
                }
            }
        }

        return false;
    }

    private static String getParameterValue(PluginExecution execution, String name, String defaultValue) {
        String value = getElementValue((Xpp3Dom) execution.getConfiguration(), name);
        return value != null ? value : defaultValue;
    }

    private static String getElementValue(Xpp3Dom config, String name) {
        if (config == null) {
            return null;
        }
        Xpp3Dom child = config.getChild(name);
        if (child == null) {
            return null;
        }
        return child.getValue();
    }
}
