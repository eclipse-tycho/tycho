/*******************************************************************************
 * Copyright (c) 2023, 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.bnd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.bndlib.SourceCodeAnalyzerPlugin;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.resolver.InstallableUnitProvider;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.MavenVersion;
import aQute.bnd.version.Version;
import aQute.lib.manifest.ManifestUtil;

/**
 * This provides the basics we need to sort the build by scanning the sourcecode for packages
 * provided and compute a preliminary manifest
 */
@Named(TychoConstants.PDE_BND)
@Singleton
public class PdeInstallableUnitProvider implements InstallableUnitProvider {

    @Inject
    private Logger logger;
    @Inject
    private TychoProjectManager projectManager;
    @Inject
    private InstallableUnitGenerator installableUnitGenerator;
    @Inject
    private MavenDependenciesResolver mavenDependenciesResolver;

    private Map<MavenProject, Collection<IInstallableUnit>> cache = new ConcurrentHashMap<>();

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        return cache.computeIfAbsent(project, p -> {
            Optional<Processor> bndTychoProject = projectManager.getBndTychoProject(project);
            if (bndTychoProject.isPresent()) {
                try (Processor pr = bndTychoProject.get()) {
                    return generateWithProcessor(project, pr, project.getArtifacts());
                } catch (Exception e) {
                    logger.warn("Can't determine additional units for " + project.getId(), e);
                }
            } else {
                //bnd maven project?
                Optional<Processor> bndPluginProcessor = getProcessor(project);
                if (bndPluginProcessor.isPresent()) {
                    try (Processor pr = bndPluginProcessor.get()) {
                        if (pr.getProperty(Constants.BUNDLE_SYMBOLICNAME) == null) {
                            pr.setProperty(Constants.BUNDLE_SYMBOLICNAME, project.getArtifactId());
                        }
                        if (pr.getProperty(Constants.BUNDLE_VERSION) == null) {
                            Version version = new MavenVersion(project.getVersion()).getOSGiVersion();
                            pr.setProperty(Constants.BUNDLE_VERSION, version.toString());
                        }
                        Collection<Dependency> dependencies = collectInitial(project, new HashMap<>()).values();
                        return generateWithProcessor(project, pr, mavenDependenciesResolver.resolve(project,
                                dependencies, Set.of(Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST), session));
                    } catch (Exception e) {
                        logger.warn("Can't determine additional units for " + project.getId(), e);
                    }
                }
            }
            return Collections.emptyList();
        });
    }

    private Optional<Processor> getProcessor(MavenProject project) {
        if ("jar".equals(project.getPackaging())) {
            Plugin plugin = project.getPlugin(Plugin.constructKey("biz.aQute.bnd", "bnd-maven-plugin"));
            if (plugin != null) {
                for (PluginExecution execution : plugin.getExecutions()) {
                    if (execution.getGoals().contains("bnd-process")) {
                        Xpp3Dom configuration = getConfig(execution.getConfiguration());
                        if (configuration != null) {
                            Properties props = new Properties();
                            Properties pp = project.getProperties();
                            for (String k : pp.stringPropertyNames()) {
                                props.setProperty(k, pp.getProperty(k));
                            }
                            File bndFile = getBndFile(configuration, project);
                            if (bndFile.exists()) {
                                try (FileInputStream stream = new FileInputStream(bndFile)) {
                                    props.load(stream);
                                } catch (IOException e) {
                                }
                            } else {
                                Xpp3Dom bnd = configuration.getChild("bnd");
                                if (bnd != null) {
                                    try {
                                        props.load(new StringReader(bnd.getValue()));
                                    } catch (IOException e) {
                                    }
                                }
                            }
                            Processor processor = new Processor(props, false);
                            processor.setBase(project.getBasedir());
                            return Optional.of(processor);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private File getBndFile(Xpp3Dom configuration, MavenProject project) {
        Xpp3Dom bndfile = configuration.getChild("bndfile");
        if (bndfile != null) {
            return new File(bndfile.getValue());
        }
        return new File(project.getBasedir(), "bnd.bnd");
    }

    private static Xpp3Dom getConfig(Object configuration) {
        if (configuration == null) {
            return null;
        }
        if (configuration instanceof Xpp3Dom) {
            return (Xpp3Dom) configuration;
        }
        try {
            return Xpp3DomBuilder.build(new StringReader(configuration.toString()));
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Dependency> collectInitial(MavenProject project, Map<String, Dependency> map) {
        for (Dependency dependency : project.getDependencies()) {
            map.putIfAbsent(dependency.getManagementKey(), dependency);
        }
        MavenProject parent = project.getParent();
        if (parent != null) {
            return collectInitial(parent, map);
        }
        return map;
    }

    private Collection<IInstallableUnit> generateWithProcessor(MavenProject project, Processor processor,
            Collection<Artifact> artifacts) throws Exception {
        SourceCodeAnalyzerPlugin plugin = new SourceCodeAnalyzerPlugin(
                project.getCompileSourceRoots().stream().map(Path::of).toList());
        try (Builder analyzer = new Builder(processor) {
            @Override
            public Clazz getPackageInfo(PackageRef packageRef) {
                Clazz info = super.getPackageInfo(packageRef);
                if (info == null) {
                    return plugin.getPackageInfoClass(packageRef);
                }
                return info;
            }

            @Override
            public Clazz findClass(TypeRef typeRef) throws Exception {
                //TODO instead of override the getPackageInfo(...) we can also use this but 
                //in that case we probably need to implement more in the JDTClazz as it is called from different places
                return super.findClass(typeRef);
            };
        }) {
            analyzer.setBase(project.getBasedir());
            Jar jar = new Jar(project.getArtifactId());
            analyzer.setJar(jar);
            for (Artifact artifact : artifacts) {
                File file = artifact.getFile();
                if (file != null) {
                    try {
                        analyzer.addClasspath(file);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            analyzer.addBasicPlugin(plugin);
            analyzer.setProperty(Constants.NOEXTRAHEADERS, "true");
            analyzer.build();
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return Collections.emptyList();
            }
            if (logger.isDebugEnabled()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ManifestUtil.write(manifest, outputStream);
                String str = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                logger.debug("Generated preliminary manifest for " + project.getId() + ":\r\n" + str);
                for (String error : analyzer.getErrors()) {
                    logger.debug("ERROR: " + error);
                }
                for (String warn : analyzer.getWarnings()) {
                    logger.debug("WARN:  " + warn);
                }
            }
            return installableUnitGenerator.getInstallableUnits(manifest);
        }
    }

}
