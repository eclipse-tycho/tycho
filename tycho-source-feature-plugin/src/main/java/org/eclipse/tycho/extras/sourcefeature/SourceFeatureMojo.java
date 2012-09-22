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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DebugUtils;
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
 * @goal source-feature
 */
public class SourceFeatureMojo extends AbstractMojo {

    public static final String FEATURE_TEMPLATE_DIR = "sourceTemplateFeature";

    public static final String SOURCES_FEATURE_CLASSIFIER = "sources-feature";

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /**
     * Bundles and features that do not have corresponding sources.
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
     * source-feature mojo versions without prio notice.
     * 
     * @parameter
     */
    @SuppressWarnings("unused")
    private PlexusConfiguration plugins;

    /**
     * @parameter default-value="${session}"
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

    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    /** @component */
    private EquinoxServiceFactory equinox;

    /** @component */
    private Logger logger;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File template = new File(project.getBasedir(), FEATURE_TEMPLATE_DIR);

        if (!ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(project.getPackaging()) || !template.isDirectory()) {
            return;
        }

        File outputJarFile = getOutputJarFile();

        TychoProject tychoProject = projectTypes.get(project.getPackaging());

        if (tychoProject == null) {
            throw new MojoExecutionException("Is not a supported tycho project " + project);
        }

        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(outputJarFile);

        try {
            File sourceFeatireDir = getSourcesFeatureDir(project);
            File featureXml = new File(sourceFeatireDir, Feature.FEATURE_XML);

            TargetPlatform targetPlatform = tychoProject.getTargetPlatform(project);

            final Feature sourceFeature = getSourceFeature(project, targetPlatform);

            Feature.write(sourceFeature, featureXml);

            DefaultFileSet mainFileSet = new DefaultFileSet();
            mainFileSet.setDirectory(template);

            archiver.getArchiver().addFileSet(mainFileSet);

            archiver.getArchiver().addFile(featureXml, Feature.FEATURE_XML);

            archiver.createArchive(project, archive);

            projectHelper.attachArtifact(project, outputJarFile, SOURCES_FEATURE_CLASSIFIER);
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not package source feature jar", e);
        }
    }

    static File getSourcesFeatureDir(MavenProject project) {
        File dir = new File(project.getBuild().getDirectory(), SOURCES_FEATURE_CLASSIFIER);
        dir.mkdirs();
        new File(dir, "p2.inf").delete();
        return dir;
    }

    private Feature getSourceFeature(MavenProject project, TargetPlatform targetPlatform) throws IOException,
            MojoExecutionException {
        P2ResolverFactory factory = equinox.getService(P2ResolverFactory.class);
        P2Resolver p2 = factory.createResolver(new MavenLoggerAdapter(logger, DebugUtils.isDebugEnabled(session,
                project)));

        Feature feature = Feature.read(new File(project.getBuild().getDirectory(), "feature.xml"));

        Document document = new Document();
        document.setRootNode(new Element("feature"));
        document.setXmlDeclaration(new XMLDeclaration("1.0", "UTF-8"));
        Feature sourceFeature = new Feature(document);
        sourceFeature.setId(feature.getId() + ".source");
        sourceFeature.setVersion(feature.getVersion());

        // make sure versions of sources and binary features match
        FeatureRef binaryRef = new FeatureRef(new Element("includes"));
        binaryRef.setId(feature.getId());
        binaryRef.setVersion(feature.getVersion());
        sourceFeature.addFeatureRef(binaryRef);

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

        return sourceFeature;
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
