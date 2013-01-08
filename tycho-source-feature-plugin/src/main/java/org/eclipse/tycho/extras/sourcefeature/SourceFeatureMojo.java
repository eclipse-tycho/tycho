/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.sourcefeature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.osgitools.DebugUtils;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLDeclaration;

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
 * <li>Includes the original feature. This ensures that binaries and corresponding sources match.</li>
 * </ul>
 * 
 * Source feature generation can be customized by adding files under path
 * <code>sourceTemplateFeature/</code>. Files added here will be added to the root of the source
 * feature jar. Especially, if file <code>sourceTemplateFeature/feature.properties</code> is found,
 * values in this file override values of respective keys in
 * <code>&lt;originalFeature&gt;/feature.properties</code>.
 * 
 * @goal source-feature
 * @phase package
 */
public class SourceFeatureMojo extends AbstractMojo {

    public static final String FEATURE_TEMPLATE_DIR = "sourceTemplateFeature";

    public static final String SOURCES_FEATURE_CLASSIFIER = "sources-feature";

    private static final String FEATURE_PROPERTIES = "feature.properties";

    private static final String GEN_DIR = "sources-feature";

    /**
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Whether to skip source feature generation.
     * 
     * @parameter default-value="false"
     */
    private boolean skip;

    /**
     * Source feature label suffix. Unless explicitly provided in
     * <code>sourceTemplateFeature/feature.properties</code>, this suffix will be appended to the
     * original feature label to construct the source feature label.
     * 
     * @parameter default-value=" Developer Resources"
     */
    private String labelSuffix;

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
     * @parameter
     */
    @SuppressWarnings("unused")
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
     * @parameter
     */
    @SuppressWarnings("unused")
    private PlexusConfiguration plugins;

    /**
     * @parameter default-value="${session}"
     * @readonly
     */
    private MavenSession session;

    private final Set<String> excludedPlugins = new HashSet<String>();

    private final Set<String> excludedFeatures = new HashSet<String>();

    private final Set<PluginRef> extraPlugins = new HashSet<PluginRef>();

    /**
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /** @component */
    private EquinoxServiceFactory equinox;

    /** @component */
    private Logger logger;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || skip) {
            return;
        }
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
            archiver.getArchiver().addFile(sourceFeatureXml, Feature.FEATURE_XML);
            archiver.getArchiver().addFile(getMergedSourceFeaturePropertiesFile(), FEATURE_PROPERTIES);
            archiver.createArchive(project, archive);

            projectHelper.attachArtifact(project, outputJarFile, SOURCES_FEATURE_CLASSIFIER);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not package source feature jar", e);
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
        Properties generatedOriginalFeatureProps = readPropertiesIfExists(new File(project.getBuild().getDirectory(),
                FEATURE_PROPERTIES));
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
        fillReferences(sourceFeature, feature, TychoProjectUtils.getTargetPlatform(project));

        Feature.write(sourceFeature, sourceFeatureXml);
        return sourceFeatureXml;
    }

    private File getMergedSourceFeaturePropertiesFile() {
        return new File(getSourcesFeatureOutputDir(project), FEATURE_PROPERTIES);
    }

    private static Properties readPropertiesIfExists(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        if (propertiesFile.isFile()) {
            FileInputStream propertiesStream = null;
            try {
                propertiesStream = new FileInputStream(propertiesFile);
                properties.load(propertiesStream);
            } finally {
                IOUtil.close(propertiesStream);
            }
        }
        return properties;
    }

    private static void writeProperties(Properties props, File propertiesFile) throws IOException {
        propertiesFile.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(propertiesFile);
        try {
            props.save(out, "");
        } finally {
            IOUtil.close(out);
        }
    }

    /**
     * This only create the new feature skeleton by setting labels and other not-structural values
     * that don't require platform resolution.
     */
    Feature createSourceFeatureSkeleton(Feature feature, Properties mergedFeatureProperties,
            Properties sourceTemplateProperties) throws IOException, MojoExecutionException {
        Document document = new Document();
        document.setRootNode(new Element("feature"));
        document.setXmlDeclaration(new XMLDeclaration("1.0", "UTF-8"));
        Feature sourceFeature = new Feature(document);
        sourceFeature.setId(feature.getId() + ".source");
        sourceFeature.setVersion(feature.getVersion());
        if (feature.getBrandingPluginId() != null) {
            sourceFeature.setBrandingPluginId(feature.getBrandingPluginId());
        }

        // make sure versions of sources and binary features match
        FeatureRef binaryRef = new FeatureRef(new Element("includes"));
        binaryRef.setId(feature.getId());
        binaryRef.setVersion(feature.getVersion());
        sourceFeature.addFeatureRef(binaryRef);

        if (feature.getLabel() != null) {
            String originalLabel = feature.getLabel();
            if (originalLabel.startsWith("%")) {
                sourceFeature.setLabel(validateValue(originalLabel, mergedFeatureProperties));
                String labelKey = originalLabel.substring(1);
                if (sourceTemplateProperties.getProperty(labelKey) == null) {
                    mergedFeatureProperties.setProperty(labelKey, mergedFeatureProperties.getProperty(labelKey)
                            + labelSuffix);
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
        if (fieldValue.charAt(0) == '%') {
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
        P2Resolver p2 = factory.createResolver(new MavenLoggerAdapter(this.logger, DebugUtils.isDebugEnabled(
                this.session, this.project)));

        List<PluginRef> missingSourcePlugins = new ArrayList<PluginRef>();
        List<FeatureRef> missingSourceFeatures = new ArrayList<FeatureRef>();
        List<PluginRef> missingExtraPlugins = new ArrayList<PluginRef>();

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

        // include available source features
        for (FeatureRef featureRef : feature.getIncludedFeatures()) {

            if (excludedFeatures.contains(featureRef.getId())) {
                continue;
            }

            String sourceId = featureRef.getId() + ".source";

            P2ResolutionResult result = p2.resolveInstallableUnit(targetPlatform, sourceId + ".feature.group",
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
        sourceRef.setDownloadSide(0);
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
        String filename = project.getArtifactId() + "-" + SOURCES_FEATURE_CLASSIFIER + ".jar";
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
        if (attr.length() == 0) {
            return null;
        }
        return attr;
    }

}
