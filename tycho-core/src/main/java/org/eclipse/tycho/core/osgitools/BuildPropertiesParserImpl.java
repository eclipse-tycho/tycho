/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Christoph Läubrich -     Bug 443083 - generating build.properties resource is not possible
 *******************************************************************************/

package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.PreDestroy;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.BuildPropertiesImpl;
import org.eclipse.tycho.core.maven.TychoInterpolator;

@Named
@Singleton
public class BuildPropertiesParserImpl implements BuildPropertiesParser {

    private final Map<String, BuildPropertiesImpl> cache = new HashMap<>();

    @Inject
    LegacySupport legacySupport;

    @Override
    public BuildProperties parse(ReactorProject project) {
        MavenProject mavenProject = project.adapt(MavenProject.class);
        return get(project.getBasedir(), () -> {
            if (mavenProject != null) {
                MavenSession session = legacySupport.getSession();
                if (session != null) {
                    return new TychoInterpolator(session, mavenProject);
                }
            }
            return null;
        }, mavenProject);
    }

    @Override
    public BuildProperties parse(File baseDir, Interpolator interpolator) {
        if (interpolator == null) {
            return get(baseDir, () -> {
                MavenSession session = legacySupport.getSession();
                if (session != null) {
                    MavenProject currentProject = session.getCurrentProject();
                    if (currentProject != null) {
                        return new TychoInterpolator(session, currentProject);
                    }
                }
                return null;
            }, null);
        }
        return get(baseDir, () -> interpolator, null);
    }

    private synchronized BuildProperties get(File baseDir, Supplier<Interpolator> interpolatorSupplier,
            MavenProject mavenProject) {
        File propsFile = new File(baseDir, BUILD_PROPERTIES);
        long lastModified = propsFile.lastModified();
        String filePath = propsFile.getAbsolutePath();
        BuildPropertiesImpl buildProperties = cache.get(filePath);
        if (buildProperties == null || lastModified > buildProperties.getTimestamp()) {
            Properties properties = readProperties(propsFile, mavenProject);
            interpolate(properties, interpolatorSupplier.get());
            buildProperties = new BuildPropertiesImpl(properties, lastModified);
            cache.put(filePath, buildProperties);
        }
        return buildProperties;
    }

    @PreDestroy
    public void dispose() {
        cache.clear();
    }

    protected static Properties readProperties(File propsFile, MavenProject mavenProject) {
        Properties properties = new Properties();
        if (propsFile.isFile()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                properties.load(is);
            } catch (IOException e) {
                // ignore
            }
        } else {
            if (mavenProject != null) {
                File basedir = mavenProject.getBasedir();
                properties.put("source..", mavenProject.getCompileSourceRoots().stream().map(p -> relative(basedir, p))
                        .collect(Collectors.joining(",")));
                properties.setProperty("output..", relative(basedir, mavenProject.getBuild().getOutputDirectory()));
                properties.setProperty("bin.includes", ".");
            }
        }

        return properties;
    }

    private static String relative(File basedir, String p) {
        String base = basedir.getAbsolutePath();
        if (p.startsWith(base)) {
            p = p.substring(base.length());
        }
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    protected void interpolate(Properties properties, Interpolator interpolator) {
        if (properties.isEmpty() || interpolator == null) {
            return;
        }
        for (Entry<Object, Object> entry : properties.entrySet()) {
            entry.setValue(interpolator.interpolate((String) entry.getValue()));
        }
    }
}
