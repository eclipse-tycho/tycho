/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.jar.JarFile;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.packaging.IncludeValidationHelper;
import org.osgi.framework.Version;

/**
 * Goal to create a JAR-package containing all the source files of a osgi project.
 */
@Mojo(name = "plugin-source", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
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
     */
    @Parameter(defaultValue = "true")
    private boolean sourceBundle;

    /**
     * Whether sources for nested jars should be put into distinct source root folders inside the
     * source jar (one source root per nested jar). E.g. if this paramater is <code>true</code> and
     * there is a nested jar named <code>foo.jar</code>, all of its sources will go into folder
     * <code>foosrc/</code>. Otherwise all sources for all jars, nested or not, will go into the
     * root of the source jar (this is the default as it provides interoperability with maven source
     * jars).
     */
    @Parameter(defaultValue = "false")
    private boolean distinctSourceRoots;

    /**
     * The suffix to be added to the symbolic name of the bundle to construct the symbolic name of
     * the Eclipse source bundle.
     */
    @Parameter(property = "sourceBundleSuffix", defaultValue = ".source")
    private String sourceBundleSuffix;

    /**
     * Build qualifier. Recommended way to set this parameter is using build-qualifier goal. Only
     * used when creating a source bundle.
     */
    @Parameter(property = "buildQualifier")
    private String qualifier;

    /**
     * Whether default source excludes for SCM files defined in
     * {@see AbstractScanner#DEFAULTEXCLUDES} should be used.
     */
    @Parameter(defaultValue = "true")
    protected boolean useDefaultSourceExcludes;

    /**
     * Whether source folders are required or not. If not required (the default), projects without
     * source folders/source includes will be silently ignored.
     */
    @Parameter(defaultValue = "false", readonly = true)
    protected boolean requireSourceRoots;

    /**
     * If set to <code>true</code> (the default), missing build.properties src.includes will cause
     * build failure. If set to <code>false</code>, missing build.properties src.includes will be
     * reported as warnings but the build will not fail.
     */
    @Parameter(defaultValue = "true")
    protected boolean strictSrcIncludes;

    /**
     * Additional files to be included in the source bundle jar. This can be used when
     * <tt>src.includes</tt> in build.properties is not flexible enough , e.g. for files which would
     * otherwise conflict with files in <tt>bin.includes</tt><br/>
     * Example:<br/>
     * 
     * <pre>
     * &lt;additionalFileSets&gt;
     *  &lt;fileSet&gt;
     *   &lt;directory&gt;${project.basedir}/sourceIncludes/&lt;/directory&gt;
     *   &lt;includes&gt;
     *    &lt;include&gt;&#42;&#42;/*&lt;/include&gt;
     *   &lt;/includes&gt;
     *  &lt;/fileSet&gt;     
     * &lt;/additionalFileSets&gt;
     * </pre>
     */
    @Parameter
    private DefaultFileSet[] additionalFileSets;

    /**
     * The source bundles classifier. The name of the source bundle will be <code>finalName-
     * classifier.jar</code>
     */
    @Parameter(defaultValue = ReactorProject.SOURCE_ARTIFACT_CLASSIFIER)
    private String classifier;

    @Component(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Component
    private IncludeValidationHelper includeValidationHelper;

    @Component
    private BundleReader bundleReader;

    /** {@inheritDoc} */
    @Override
    protected List<Resource> getSources(MavenProject p) throws MojoExecutionException {
        return getSources(project, requireSourceRoots, DefaultReactorProject.adapt(project).getBuildProperties());
    }

    protected List<Resource> getSources(MavenProject p, boolean requireSourceRoots, BuildProperties buildProperties)
            throws MojoExecutionException {
        List<Resource> resources = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : buildProperties.getJarToSourceFolderMap().entrySet()) {
            for (String sourceFolder : entry.getValue()) {
                Resource resource = new Resource();
                resource.setDirectory(new File(p.getBasedir(), sourceFolder).getAbsolutePath());
                if (distinctSourceRoots) {
                    String jarName = entry.getKey();
                    if (!".".equals(jarName)) {
                        resource.setTargetPath(getSourceRootTargetPath(jarName));
                    }
                }
                resources.add(resource);
            }
        }

        if (requireSourceRoots && resources.isEmpty()) {
            throw new MojoExecutionException("no source folders found in build.properties");
        }
        return resources;
    }

    /** {@inheritDoc} */
    @Override
    protected List<Resource> getResources(MavenProject p) throws MojoExecutionException {
        if (excludeResources) {
            return Collections.emptyList();
        }
        BuildProperties buildProperties = DefaultReactorProject.adapt(p).getBuildProperties();
        List<String> srcIncludesList = buildProperties.getSourceIncludes();
        List<Resource> resources = new ArrayList<>();
        if (!srcIncludesList.isEmpty()) {
            includeValidationHelper.checkSourceIncludesExist(p, buildProperties, strictSrcIncludes);
            resources.add(createResource(project.getBasedir().getAbsolutePath(), srcIncludesList,
                    buildProperties.getSourceExcludes()));
        }
        if (additionalFileSets != null) {
            for (DefaultFileSet fileSet : additionalFileSets) {
                if (fileSet.getIncludes() != null && fileSet.getIncludes().length > 0) {
                    resources.add(createResource(fileSet.getDirectory().getAbsolutePath(),
                            asList(fileSet.getIncludes()), asList(fileSet.getExcludes())));
                }
            }
        }
        if (!srcIncludesList.contains(MANIFEST_BUNDLE_LOCALIZATION_FILENAME)) {
            resources.add(generateL10nFile());
        }
        return resources;
    }

    private static List<String> asList(String[] patterns) {
        if (patterns == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(patterns);
    }

    private static Resource createResource(String directory, List<String> includes, List<String> excludes) {
        Resource resource = new Resource();
        resource.setDirectory(directory);
        resource.setExcludes(excludes);
        resource.setIncludes(includes);
        return resource;
    }

    private Resource generateL10nFile() throws MojoExecutionException {
        OsgiManifest origManifest = bundleReader.loadManifest(project.getBasedir());
        Properties l10nProps = readL10nProps(origManifest);
        String bundleName = getL10nResolvedValue(origManifest, BUNDLE_NAME, l10nProps);
        if (bundleName == null) {
            getLog().warn("Bundle-Name header not found in " + new File(project.getBasedir(), JarFile.MANIFEST_NAME)
                    + ", fallback to Bundle-SymbolicName for source bundle");
            bundleName = origManifest.getBundleSymbolicName();
        }
        String sourceBundleName = bundleName + " Source";
        String bundleVendor = getL10nResolvedValue(origManifest, BUNDLE_VENDOR, l10nProps);
        if (bundleVendor == null) {
            getLog().warn("Bundle-Vendor header not found in " + new File(project.getBasedir(), JarFile.MANIFEST_NAME)
                    + ", fallback to 'unknown' for source bundle");
            bundleVendor = "unknown";
        }
        File l10nOutputDir = new File(project.getBuild().getDirectory(), "sourcebundle-l10n-gen");
        Properties sourceL10nProps = new Properties();
        sourceL10nProps.setProperty(I18N_KEY_BUNDLE_NAME, sourceBundleName);
        sourceL10nProps.setProperty(I18N_KEY_BUNDLE_VENDOR, bundleVendor);
        File l10nPropsFile = new File(l10nOutputDir, MANIFEST_BUNDLE_LOCALIZATION_FILENAME);
        l10nPropsFile.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(l10nPropsFile)) {
            sourceL10nProps.store(out, "Source Bundle Localization");
        } catch (IOException e) {
            throw new MojoExecutionException("error while generating source bundle localization file", e);
        }
        Resource l10nResource = new Resource();
        l10nResource.setDirectory(l10nOutputDir.getAbsolutePath());
        l10nResource.setIncludes(singletonList(MANIFEST_BUNDLE_LOCALIZATION_FILENAME));
        return l10nResource;
    }

    protected Properties readL10nProps(OsgiManifest manifest) throws MojoExecutionException {
        String bundleL10nBase = manifest.getValue(BUNDLE_LOCALIZATION);
        boolean hasL10nProperty = bundleL10nBase != null;
        if (bundleL10nBase == null) {
            bundleL10nBase = BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
        }
        File l10nPropsFile = new File(project.getBasedir(), bundleL10nBase + ".properties");
        if (!l10nPropsFile.isFile()) {
            bundleL10nBase = "plugin";
            l10nPropsFile = new File(project.getBasedir(), bundleL10nBase + ".properties");
            if (!l10nPropsFile.isFile()) {
                if (hasL10nProperty) {
                    getLog().warn("bundle localization file " + l10nPropsFile + " not found");
                }
                return null;
            }
        }
        Properties l10nProps = new Properties();
        try (FileInputStream in = new FileInputStream(l10nPropsFile)) {
            l10nProps.load(in);
        } catch (IOException e) {
            throw new MojoExecutionException("error loading " + l10nPropsFile, e);
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
    @Override
    protected String getClassifier() {
        return classifier;
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
                    + ";version=\"" + expandedVersion + "\";roots:=\"" + getEclipseHeaderSourceRoots() + "\"");

            mavenArchiveConfiguration.addManifestEntry(BUNDLE_NAME, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_NAME);
            mavenArchiveConfiguration.addManifestEntry(BUNDLE_VENDOR, I18N_KEY_PREFIX + I18N_KEY_BUNDLE_VENDOR);
            mavenArchiveConfiguration.addManifestEntry(BUNDLE_LOCALIZATION, MANIFEST_BUNDLE_LOCALIZATION_BASENAME);
        } else {
            getLog().info("NOT adding source bundle manifest entries. Incomplete or no bundle information available.");
        }
    }

    private String getEclipseHeaderSourceRoots() {
        if (!distinctSourceRoots) {
            return ".";
        }
        StringJoiner result = new StringJoiner(",");
        for (String jarName : DefaultReactorProject.adapt(project).getBuildProperties().getJarToSourceFolderMap()
                .keySet()) {
            String sourceRoot;
            if (".".equals(jarName)) {
                sourceRoot = ".";
            } else {
                sourceRoot = getSourceRootTargetPath(jarName);
            }
            result.add(sourceRoot);
        }
        return result.toString();
    }

    private static String getSourceRootTargetPath(String jarName) {
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - ".jar".length());
        } else if (jarName.endsWith("/")) {
            jarName = jarName.substring(0, jarName.length() - 1);
        }
        return jarName + "src";
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
        return isRelevant(project);
    }

    public static boolean isRelevant(MavenProject project) {
        String packaging = project.getPackaging();
        boolean relevant = PackagingType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
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
                boolean requireSourceRoots = Boolean
                        .parseBoolean(getParameterValue(execution, "requireSourceRoots", "false"));
                if (requireSourceRoots) {
                    return true;
                }
                boolean hasAdditionalFilesets = getConfigurationElement((Xpp3Dom) execution.getConfiguration(),
                        "additionalFileSets") != null;
                if (hasAdditionalFilesets) {
                    return true;
                }
                BuildProperties buildProperties = DefaultReactorProject.adapt(project).getBuildProperties();
                if (buildProperties.getJarToSourceFolderMap().size() > 0
                        || buildProperties.getSourceIncludes().size() > 0) {
                    return true;
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
        Xpp3Dom child = getConfigurationElement(config, name);
        if (child == null) {
            return null;
        }
        return child.getValue();
    }

    private static Xpp3Dom getConfigurationElement(Xpp3Dom config, String name) {
        if (config == null) {
            return null;
        }
        Xpp3Dom child = config.getChild(name);
        return child;
    }

}
