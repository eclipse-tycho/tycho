/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann GmbH. - Bug 538395 Generate valid feature xml 
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.packaging.LicenseFeatureHelper;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

/**
 * Generates a source feature for projects of packaging type <code>eclipse-feature</code>. By
 * default, the generated source feature
 * <ul>
 * <li>Declares feature id <code>&lt;originalFeatureId&gt;.source</code></li>
 * <li>Declares feature label <code>"&lt;originalFeatureLabel&gt; Developer Resources"</code></li>
 * <li>If present, reuses feature provider, description, copyright and license as well as respective
 * URLs from &lt;originalFeature&gt;</li>
 * <li>Includes all plugins included by &lt;originalFeature&gt;, but each with <code>.source</code>
 * appended to each plugin id</li>
 * <li>Includes all features included by &lt;originalFeature&gt;, but each with <code>.source</code>
 * appended to each feature id</li>
 * <li>Includes the original feature. This ensures that binaries and corresponding sources
 * match.</li>
 * </ul>
 * 
 * Source feature generation can be customized by adding files under path
 * <code>sourceTemplateFeature/</code>. Files added here will be added to the root of the source
 * feature jar. Especially, if file <code>sourceTemplateFeature/feature.properties</code> is found,
 * values in this file override values of respective keys in
 * <code>&lt;originalFeature&gt;/feature.properties</code>.
 * 
 * @deprecated this mojo is replaced by the tycho-source-plugin with execution feature-source which
 *             offers equivalent and even enhanced functionality
 */
@Mojo(name = "source-feature", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
@Deprecated(since = "2.2", forRemoval = true)
public class SourceFeatureMojo extends AbstractMojo {

    /**
     * Lock object to ensure thread-safety
     */
    private static final Object LOCK = new Object();

    public static final String FEATURE_TEMPLATE_DIR = "sourceTemplateFeature";

    public static final String SOURCES_FEATURE_CLASSIFIER = "sources-feature";

    private static final String FEATURE_PROPERTIES = "feature.properties";

    private static final String GEN_DIR = "sources-feature";

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    /**
     * Whether to skip source feature generation.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * Whether to add an include dependency from the source feature to the corresponding binary
     * feature. If <code>true</code>, this ensures the version of the installed sources matches the
     * binaries.
     */
    @Parameter(defaultValue = "true")
    private boolean includeBinaryFeature;

    /**
     * Source feature label suffix. Unless explicitly provided in
     * <code>sourceTemplateFeature/feature.properties</code>, this suffix will be appended to the
     * original feature label to construct the source feature label.
     */
    @Parameter(defaultValue = " Developer Resources")
    private String labelSuffix;

    /**
     * Use this to explicitly set the <a href=
     * "https://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Ffeature_editor%2Ffeature_editor.htm"
     * > branding plugin attribute</a> of the generated source feature (overrides
     * {@link #reuseBrandingPlugin}).
     */
    @Parameter
    private String brandingPlugin;

    /**
     * Whether to reuse an explicit branding plugin from the binary feature for the generated source
     * feature.
     */
    @Parameter(defaultValue = "true")
    private boolean reuseBrandingPlugin;

    /**
     * Bundles and features that do not have corresponding sources. Example:
     * 
     * <pre>
     * &lt;excludes&gt;
     *   &lt;plugin id="plugin.nosource"/&gt;
     *   &lt;feature id="feature.nosource"/&gt;
     * &lt;/excludes&gt;
     * </pre>
     * 
     */
    @Parameter
    private PlexusConfiguration excludes;

    /**
     * Additional plugins to include in the generated source feature. Beware that these additional
     * plugins are not considered during build target platform calculation and ordering of reactor
     * projects. Use &lt;extraRequirements&gt; dependency resolver configuration to guarantee proper
     * reactor build order.
     * <p>
     * <strong>WARNING</strong> This experimental parameter may be removed from future
     * source-feature mojo versions without prior notice.
     * 
     */
    @Parameter
    private PlexusConfiguration plugins;

    @Parameter(defaultValue = "true")
    protected boolean useDefaultExcludes;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    private final Set<String> excludedPlugins = new HashSet<>();

    private final Set<String> excludedFeatures = new HashSet<>();

    private final Set<PluginRef> extraPlugins = new HashSet<>();

    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * The filename to be used for the generated archive file. For the source-feature goal,
     * "-sources-feature" is appended to this filename.
     */
    @Parameter(property = "project.build.finalName")
    private String finalName;

    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    @Component
    private MavenProjectHelper projectHelper;

    @Component
    private LicenseFeatureHelper licenseFeatureHelper;

    @Component
    private BuildPropertiesParser buildPropertiesParser;

    @Component
    private EquinoxServiceFactory equinox;

    @Component
    private Logger logger;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.warn(
                "Mojo tycho-source-feature-plugin:source-feature is replaced by the tycho-source-plugin:feature-source which offers equivalent and even enhanced functionality");
        if (!PackagingType.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || skip) {
            return;
        }
        synchronized (LOCK) {
            try {
                Properties sourceFeatureTemplateProps = readSourceTemplateFeatureProperties();
                Properties mergedSourceFeatureProps = mergeFeatureProperties(sourceFeatureTemplateProps);
                File sourceFeatureXml = generateSourceFeatureXml(mergedSourceFeatureProps, sourceFeatureTemplateProps);
                writeProperties(mergedSourceFeatureProps, getMergedSourceFeaturePropertiesFile());
                MavenArchiver archiver = new MavenArchiver();
                archiver.setArchiver(jarArchiver);
                File outputJarFile = getOutputJarFile();
                archiver.setOutputFile(outputJarFile);
                File template = new File(project.getBasedir(), FEATURE_TEMPLATE_DIR);
                if (template.isDirectory()) {
                    DefaultFileSet templateFileSet = new DefaultFileSet();
                    templateFileSet.setDirectory(template);
                    // make sure we use generated feature.xml and feature.properties
                    templateFileSet.setExcludes(new String[] { Feature.FEATURE_XML, FEATURE_PROPERTIES });
                    archiver.getArchiver().addFileSet(templateFileSet);
                }

                BuildProperties buildProperties = buildPropertiesParser.parse(project.getBasedir());
                archiver.getArchiver().addFileSet(getManuallyIncludedFiles(project.getBasedir(), buildProperties));

                archiver.getArchiver().addFile(sourceFeatureXml, Feature.FEATURE_XML);
                archiver.getArchiver().addFile(getMergedSourceFeaturePropertiesFile(), FEATURE_PROPERTIES);
                File licenseFeature = licenseFeatureHelper
                        .getLicenseFeature(Feature.read(new File(project.getBasedir(), "feature.xml")), project);
                if (licenseFeature != null) {
                    archiver.getArchiver()
                            .addArchivedFileSet(licenseFeatureHelper.getLicenseFeatureFileSet(licenseFeature));
                }
                archiver.createArchive(session, project, archive);

                projectHelper.attachArtifact(project, outputJarFile, SOURCES_FEATURE_CLASSIFIER);
            } catch (MojoExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new MojoExecutionException("Could not package source feature jar", e);
            }
        }
    }

    static File getSourcesFeatureOutputDir(MavenProject project) {
        File dir = new File(project.getBuild().getDirectory(), GEN_DIR);
        dir.mkdirs();
        // TODO why is this needed?
        new File(dir, "p2.inf").delete();
        return dir;
    }

    private Properties mergeFeatureProperties(Properties sourceFeatureTemplateProps) throws IOException {
        Properties generatedOriginalFeatureProps = readPropertiesIfExists(
                new File(project.getBuild().getDirectory(), FEATURE_PROPERTIES));
        Properties mergedProperties = new Properties();
        mergedProperties.putAll(generatedOriginalFeatureProps);
        mergedProperties.putAll(sourceFeatureTemplateProps);
        return mergedProperties;
    }

    private Properties readSourceTemplateFeatureProperties() throws IOException {
        return readPropertiesIfExists(new File(project.getBasedir(), FEATURE_TEMPLATE_DIR + "/" + FEATURE_PROPERTIES));
    }

    private File generateSourceFeatureXml(Properties mergedSourceFeatureProps, Properties sourceTemplateProps)
            throws IOException, MojoExecutionException {
        File sourceFeatureXml = new File(getSourcesFeatureOutputDir(project), Feature.FEATURE_XML);
        Feature feature = Feature.read(new File(this.project.getBuild().getDirectory(), "feature.xml"));

        final Feature sourceFeature = createSourceFeatureSkeleton(feature, mergedSourceFeatureProps,
                sourceTemplateProps);
        fillReferences(sourceFeature, feature,
                TychoProjectUtils.getTargetPlatform(DefaultReactorProject.adapt(project)));

        Feature.write(sourceFeature, sourceFeatureXml, "  ");
        return sourceFeatureXml;
    }

    private File getMergedSourceFeaturePropertiesFile() {
        return new File(getSourcesFeatureOutputDir(project), FEATURE_PROPERTIES);
    }

    private static Properties readPropertiesIfExists(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        if (propertiesFile.isFile()) {
            try (FileInputStream propertiesStream = new FileInputStream(propertiesFile)) {
                properties.load(propertiesStream);
            }
        }
        return properties;
    }

    private static void writeProperties(Properties props, File propertiesFile) throws IOException {
        propertiesFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(propertiesFile)) {
            props.store(out, "");
        }
    }

    /**
     * This only create the new feature skeleton by setting labels and other not-structural values
     * that don't require platform resolution.
     */
    Feature createSourceFeatureSkeleton(Feature feature, Properties mergedFeatureProperties,
            Properties sourceTemplateProperties) throws IOException, MojoExecutionException {
        Document document = new Document();
        document.setEncoding("UTF-8");
        document.setRootNode(new Element("feature"));
        Feature sourceFeature = new Feature(document);
        sourceFeature.setId(feature.getId() + ".source");
        sourceFeature.setVersion(feature.getVersion());
        if (reuseBrandingPlugin && brandingPlugin == null) {
            if (feature.getBrandingPluginId() != null) {
                sourceFeature.setBrandingPluginId(feature.getBrandingPluginId());
            }
        } else if (brandingPlugin != null) {
            sourceFeature.setBrandingPluginId(brandingPlugin);
        }

        if (feature.getLabel() != null) {
            String originalLabel = feature.getLabel();
            if (originalLabel.startsWith("%")) {
                sourceFeature.setLabel(validateValue(originalLabel, mergedFeatureProperties));
                String labelKey = originalLabel.substring(1);
                if (sourceTemplateProperties.getProperty(labelKey) == null) {
                    mergedFeatureProperties.setProperty(labelKey,
                            mergedFeatureProperties.getProperty(labelKey) + labelSuffix);
                } else {
                    // keep source template value 
                }
            } else {
                sourceFeature.setLabel(originalLabel + labelSuffix);
            }
        }
        if (feature.getProvider() != null) {
            sourceFeature.setProvider(validateValue(feature.getProvider(), mergedFeatureProperties));
        }
        if (feature.getDescription() != null) {
            sourceFeature.setDescription(validateValue(feature.getDescription(), mergedFeatureProperties));
        }
        if (feature.getDescriptionURL() != null) {
            sourceFeature.setDescriptionURL(validateValue(feature.getDescriptionURL(), mergedFeatureProperties));
        }
        if (feature.getCopyright() != null) {
            sourceFeature.setCopyright(validateValue(feature.getCopyright(), mergedFeatureProperties));
        }
        if (feature.getCopyrightURL() != null) {
            sourceFeature.setCopyrightURL(validateValue(feature.getCopyrightURL(), mergedFeatureProperties));
        }
        if (feature.getLicense() != null) {
            sourceFeature.setLicense(validateValue(feature.getLicense(), mergedFeatureProperties));
        }
        if (feature.getLicenseURL() != null) {
            sourceFeature.setLicenseURL(validateValue(feature.getLicenseURL(), mergedFeatureProperties));
        }

        if (includeBinaryFeature) {
            FeatureRef binaryRef = new FeatureRef(new Element("includes"));
            binaryRef.setId(feature.getId());
            binaryRef.setVersion(feature.getVersion());
            if (feature.getOS() != null) {
                binaryRef.setOS(feature.getOS());
            }
            if (feature.getWS() != null) {
                binaryRef.setWS(feature.getWS());
            }
            if (feature.getArch() != null) {
                binaryRef.setArch(feature.getArch());
            }
            sourceFeature.addFeatureRef(binaryRef);
        }

        return sourceFeature;
    }

    /**
     * Returns the value for a field. In case the value is a reference to feature.properties, verify
     * that the entry exist in the feature.properties file for source
     * 
     * @param fieldValue
     * @param sourceFeatureProperties
     * @return
     */
    private static String validateValue(String fieldValue, Properties sourceFeatureProperties)
            throws MojoExecutionException {
        if (fieldValue.startsWith("%")) {
            String key = fieldValue.substring(1);
            if (!sourceFeatureProperties.containsKey(key)) {
                throw new MojoExecutionException("Source feature depends on '" + FEATURE_TEMPLATE_DIR
                        + "/feature.properties', entry '" + key + "'. However, this key could not be found");
            }
        }
        return fieldValue;
    }

    /**
     * Added all references to sourceFeature, as deduced by feature and resolved by targetPlatform
     * 
     * @param sourceFeature
     * @param feature
     * @param targetPlatform
     * @throws MojoExecutionException
     */
    private void fillReferences(Feature sourceFeature, Feature feature, TargetPlatform targetPlatform)
            throws MojoExecutionException {
        P2ResolverFactory factory = this.equinox.getService(P2ResolverFactory.class);
        P2Resolver p2 = factory.createResolver(
                new MavenLoggerAdapter(this.logger, DebugUtils.isDebugEnabled(this.session, this.project)));

        List<PluginRef> missingSourcePlugins = new ArrayList<>();
        List<FeatureRef> missingSourceFeatures = new ArrayList<>();
        List<PluginRef> missingExtraPlugins = new ArrayList<>();

        // include available source features
        for (FeatureRef featureRef : feature.getIncludedFeatures()) {

            if (excludedFeatures.contains(featureRef.getId())) {
                continue;
            }

            String sourceId = featureRef.getId() + ".source";

            // TODO 412416 either directly work on IUs (-> omit the "toResolutionResult" conversion), or ask for the Tycho artifact type ArtifactKey.TYPE_ECLIPSE_PLUGIN
            P2ResolutionResult result = p2.resolveInstallableUnit(targetPlatform, sourceId + ".feature.jar",
                    toStrictVersionRange(featureRef.getVersion()));
            if (result.getArtifacts().size() == 1) {
                Entry entry = result.getArtifacts().iterator().next();

                FeatureRef sourceRef = new FeatureRef(new Element("includes"));
                sourceRef.setId(sourceId);
                sourceRef.setVersion(entry.getVersion());
                sourceFeature.addFeatureRef(sourceRef);
            } else {
                missingSourceFeatures.add(featureRef);
            }
        }

        // include available source bundles
        for (PluginRef pluginRef : feature.getPlugins()) {

            if (excludedPlugins.contains(pluginRef.getId())) {
                continue;
            }

            // version is expected to be fully expanded at this point
            P2ResolutionResult result = p2.resolveInstallableUnit(targetPlatform, pluginRef.getId() + ".source",
                    toStrictVersionRange(pluginRef.getVersion()));
            if (result.getArtifacts().size() == 1) {
                addPlugin(sourceFeature, result, pluginRef);
            } else {
                missingSourcePlugins.add(pluginRef);
            }
        }

        for (PluginRef pluginRef : extraPlugins) {
            // version is expected to be fully expanded at this point
            P2ResolutionResult result = p2.resolveInstallableUnit(targetPlatform, pluginRef.getId(),
                    pluginRef.getVersion());
            if (result.getArtifacts().size() == 1) {
                addPlugin(sourceFeature, result, pluginRef);
            } else {
                missingExtraPlugins.add(pluginRef);
            }
        }

        if (!missingSourceFeatures.isEmpty() || !missingSourcePlugins.isEmpty() || !missingExtraPlugins.isEmpty()) {
            StringBuilder sb = new StringBuilder();

            sb.append("Could not generate source feature for project " + project.toString()).append("\n");

            if (!missingSourcePlugins.isEmpty()) {
                sb.append("    Missing sources for plugins " + missingSourcePlugins.toString()).append("\n");
            }

            if (!missingSourceFeatures.isEmpty()) {
                sb.append("    Missing sources for features " + missingSourceFeatures.toString()).append("\n");
            }

            if (!missingExtraPlugins.isEmpty()) {
                sb.append("    Missing extra plugins " + missingExtraPlugins.toString()).append("\n");
            }

            throw new MojoExecutionException(sb.toString());
        }

    }

    protected String toStrictVersionRange(String version) {
        return "[" + version + "," + version + "]";
    }

    protected void addPlugin(Feature sourceFeature, P2ResolutionResult result, PluginRef pluginRef) {
        Entry sourceBundle = result.getArtifacts().iterator().next();

        PluginRef sourceRef = new PluginRef("plugin");
        sourceRef.setId(sourceBundle.getId());
        sourceRef.setVersion(sourceBundle.getVersion());
        sourceRef.setDownloadSize(0);
        sourceRef.setInstallSize(0);
        if (pluginRef.getOs() != null) {
            sourceRef.setOs(pluginRef.getOs());
        }
        if (pluginRef.getWs() != null) {
            sourceRef.setWs(pluginRef.getWs());
        }
        if (pluginRef.getArch() != null) {
            sourceRef.setArch(pluginRef.getArch());
        }
        sourceRef.setUnpack(false);

        sourceFeature.addPlugin(sourceRef);
    }

    protected File getOutputJarFile() {
        String filename = finalName + "-" + SOURCES_FEATURE_CLASSIFIER + ".jar";
        return new File(project.getBuild().getDirectory(), filename);
    }

    // this is called by maven to inject value of <excludes> configuration element
    public void setExcludes(PlexusConfiguration excludes) {
        for (PlexusConfiguration plugin : excludes.getChildren("plugin")) {
            String id = getAttribute(plugin, "id");
            if (id != null) {
                excludedPlugins.add(id);
            }
            // TODO warn about elements with null id
        }
        for (PlexusConfiguration plugin : excludes.getChildren("feature")) {
            String id = getAttribute(plugin, "id");
            if (id != null) {
                excludedFeatures.add(id);
            }
            // TODO warn about elements with null id
        }
    }

    // this is called by maven to inject value of <excludes> configuration element
    public void setPlugins(PlexusConfiguration bundles) {
        for (PlexusConfiguration plugin : bundles.getChildren("plugin")) {
            String id = getAttribute(plugin, "id");
            if (id != null) {
                String version = getAttribute(plugin, "version");
                if (version == null) {
                    version = "0.0.0";
                }
                PluginRef ref = new PluginRef("plugin");
                ref.setId(id);
                ref.setVersion(version);
                extraPlugins.add(ref);
            }
            // TODO fail if duplicate plugins
            // TODO warn about elements with null id
        }
    }

    private String getAttribute(PlexusConfiguration dom, String attrName) {
        String attr = dom.getAttribute(attrName);
        if (attr == null) {
            return null;
        }
        attr = attr.trim();
        if (attr.isEmpty()) {
            return null;
        }
        return attr;
    }

    /**
     * @return A {@link FileSet} including files as configured by the <tt>src.includes</tt> and
     *         <tt>src.excludes</tt> properties without the files that are always included
     *         automatically.
     */
    private FileSet getManuallyIncludedFiles(File basedir, BuildProperties buildProperties) {
        List<String> srcExcludes = new ArrayList<>(buildProperties.getSourceExcludes());
        srcExcludes.add(Feature.FEATURE_XML); // we'll include updated feature.xml
        srcExcludes.add(FEATURE_PROPERTIES); // we'll include updated feature.properties
        return getFileSet(basedir, buildProperties.getSourceIncludes(), srcExcludes);
    }

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
}
