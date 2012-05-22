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

import static java.util.Collections.singletonList;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
import static org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION;
import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.packaging.IncludeValidationHelper;
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

    private static final String MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE = "Eclipse-SourceBundle";
    private static final String MANIFEST_BUNDLE_LOCALIZATION_BASENAME = BUNDLE_LOCALIZATION_DEFAULT_BASENAME + "-src";
    private static final String MANIFEST_BUNDLE_LOCALIZATION_FILENAME = MANIFEST_BUNDLE_LOCALIZATION_BASENAME
            + ".properties";
    private static final String I18N_KEY_PREFIX = "%";
    private static final String I18N_KEY_BUNDLE_VENDOR = "bundleVendor";
    private static final String I18N_KEY_BUNDLE_NAME = "bundleName";

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
     * If set to <code>true</code> (the default), missing build.properties src.includes will cause
     * build failure. If set to <code>false</code>, missing build.properties src.includes will be
     * reported as warnings but the build will not fail.
     * 
     * @parameter default-value="true"
     */
    protected boolean strictSrcIncludes;

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /**
     * @component
     */
    private BuildPropertiesParser buildPropertiesParser;

    /**
     * @component
     */
    private IncludeValidationHelper includeValidationHelper;

    /**
     * @component
     */
    private BundleReader bundleReader;

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
        List<Resource> resources = new ArrayList<Resource>();
        if (!srcIncludesList.isEmpty()) {
            includeValidationHelper.checkSourceIncludesExist(p, buildProperties, strictSrcIncludes);
            Resource resource = new Resource();
            resource.setDirectory(project.getBasedir().getAbsolutePath());
            resource.setExcludes(buildProperties.getSourceExcludes());
            resource.setIncludes(srcIncludesList);
            resources.add(resource);
        }
        if (!srcIncludesList.contains(MANIFEST_BUNDLE_LOCALIZATION_FILENAME)) {
            resources.add(generateL10nFile());
        }
        return resources;
    }

    private Resource generateL10nFile() throws MojoExecutionException {
        OsgiManifest origManifest = bundleReader.loadManifest(project.getBasedir());
        Properties l10nProps = readL10nProps(origManifest);
        String bundleName = getL10nResolvedValue(origManifest, BUNDLE_NAME, l10nProps);
        if (bundleName == null) {
            getLog().warn(
                    "Bundle-Name header not found in " + new File(project.getBasedir(), JarFile.MANIFEST_NAME)
                            + ", fallback to Bundle-SymbolicName for source bundle");
            bundleName = origManifest.getBundleSymbolicName();
        }
        String sourceBundleName = bundleName + " Source";
        String bundleVendor = getL10nResolvedValue(origManifest, BUNDLE_VENDOR, l10nProps);
        if (bundleVendor == null) {
            getLog().warn(
                    "Bundle-Vendor header not found in " + new File(project.getBasedir(), JarFile.MANIFEST_NAME)
                            + ", fallback to 'unknown' for source bundle");
            bundleVendor = "unknown";
        }
        File l10nOutputDir = new File(project.getBuild().getDirectory(), "sourcebundle-l10n-gen");
        Properties sourceL10nProps = new Properties();
        sourceL10nProps.setProperty(I18N_KEY_BUNDLE_NAME, sourceBundleName);
        sourceL10nProps.setProperty(I18N_KEY_BUNDLE_VENDOR, bundleVendor);
        File l10nPropsFile = new File(l10nOutputDir, MANIFEST_BUNDLE_LOCALIZATION_FILENAME);
        l10nPropsFile.getParentFile().mkdirs();
        OutputStream out = null;
        try {
            out = new FileOutputStream(l10nPropsFile);
            sourceL10nProps.store(out, "Source Bundle Localization");
        } catch (IOException e) {
            throw new MojoExecutionException("error while generating source bundle localization file", e);
        } finally {
            IOUtil.close(out);
        }
        Resource l10nResource = new Resource();
        l10nResource.setDirectory(l10nOutputDir.getAbsolutePath());
        l10nResource.setIncludes(singletonList(MANIFEST_BUNDLE_LOCALIZATION_FILENAME));
        return l10nResource;
    }

    private Properties readL10nProps(OsgiManifest manifest) throws MojoExecutionException {
        String bundleL10nBase = manifest.getValue(BUNDLE_LOCALIZATION);
        if (bundleL10nBase == null) {
            bundleL10nBase = "plugin";
        }
        File l10nPropsFile = new File(project.getBasedir(), bundleL10nBase + ".properties");
        if (!l10nPropsFile.isFile()) {
            getLog().warn("bundle localization file " + l10nPropsFile + " not found");
            return null;
        }
        Properties l10nProps = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(l10nPropsFile);
            l10nProps.load(in);
        } catch (IOException e) {
            throw new MojoExecutionException("error loading " + l10nPropsFile, e);
        } finally {
            IOUtil.close(in);
        }
        return l10nProps;
    }

    private String getL10nResolvedValue(OsgiManifest manifest, String manifestHeaderKey, Properties l10nProps)
            throws MojoExecutionException {
        String value = manifest.getValue(manifestHeaderKey);
        if (value == null || !value.startsWith("%")) {
            return value;
        }
        if (l10nProps == null) {
            return null;
        }
        String key = value.substring(1).trim();
        return l10nProps.getProperty(key);
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
            mavenArchiveConfiguration.addManifestEntry(BUNDLE_MANIFESTVERSION, "2");

            mavenArchiveConfiguration.addManifestEntry(BUNDLE_SYMBOLICNAME, symbolicName + sourceBundleSuffix);

            Version expandedVersion = getExpandedVersion(version);

            mavenArchiveConfiguration.addManifestEntry(BUNDLE_VERSION, expandedVersion.toString());

            mavenArchiveConfiguration.addManifestEntry(MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE, symbolicName
                    + ";version=\"" + expandedVersion + "\";roots:=\".\"");

            mavenArchiveConfiguration.addManifestEntry(BUNDLE_NAME, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_NAME);
            mavenArchiveConfiguration.addManifestEntry(BUNDLE_VENDOR, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_VENDOR);
            mavenArchiveConfiguration.addManifestEntry(BUNDLE_LOCALIZATION, MANIFEST_BUNDLE_LOCALIZATION_BASENAME);
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
