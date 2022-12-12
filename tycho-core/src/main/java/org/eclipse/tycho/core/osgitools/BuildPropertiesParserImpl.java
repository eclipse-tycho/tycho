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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.BuildProperties;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.maven.TychoInterpolator;
import org.eclipse.tycho.core.shared.BuildPropertiesImpl;

@Component(role = BuildPropertiesParser.class)
public class BuildPropertiesParserImpl implements BuildPropertiesParser, Disposable {

    private final Map<String, BuildPropertiesImpl> cache = new HashMap<>();

    @Requirement
    LegacySupport legacySupport;

    @Override
    public BuildProperties parse(ReactorProject project) {
        return get(project.getBasedir(), () -> {
            MavenProject mavenProject = project.adapt(MavenProject.class);
            if (mavenProject != null) {
                MavenSession session = legacySupport.getSession();
                if (session != null) {
                    return new TychoInterpolator(session, mavenProject);
                }
            }
            return null;
        });
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
            });
        }
        return get(baseDir, () -> interpolator);
    }

    private synchronized BuildProperties get(File baseDir, Supplier<Interpolator> interpolatorSupplier) {
        File propsFile = new File(baseDir, BUILD_PROPERTIES);
        long lastModified = propsFile.lastModified();
        String filePath = propsFile.getAbsolutePath();
        BuildPropertiesImpl buildProperties = cache.get(filePath);
        if (buildProperties == null || lastModified > buildProperties.getTimestamp()) {
            Properties properties = readProperties(propsFile);
            interpolate(properties, interpolatorSupplier.get());
            buildProperties = new BuildPropertiesImpl(properties, lastModified);
            cache.put(filePath, buildProperties);
        }
        return buildProperties;
    }

    @Override
    public void dispose() {
        cache.clear();
    }

    protected static Properties readProperties(File propsFile) {
        Properties properties = new Properties();
        if (propsFile.canRead()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                properties.load(is);
            } catch (IOException e) {
                // ignore
            }
        }

        return properties;
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
