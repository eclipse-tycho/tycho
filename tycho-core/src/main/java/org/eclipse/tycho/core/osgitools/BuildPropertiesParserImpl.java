/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.core.maven.InterpolatorFactory;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesImpl;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.shared.LRUCache;
import org.eclipse.tycho.core.utils.MavenSessionUtils;

@Component(role = BuildPropertiesParser.class)
public class BuildPropertiesParserImpl implements BuildPropertiesParser, Disposable {

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private InterpolatorFactory interpolatorFactory;

    @Requirement
    private Logger logger;

    private final LRUCache<String, BuildProperties> cache = new LRUCache<String, BuildProperties>(50);

    public BuildProperties parse(File baseDir) {
        File propsFile = new File(baseDir, BUILD_PROPERTIES);
        String filePath = propsFile.getAbsolutePath();
        BuildProperties buildProperties = cache.get(filePath);
        if (buildProperties == null) {
            Properties properties = readProperties(propsFile);
            interpolate(properties, baseDir);
            buildProperties = new BuildPropertiesImpl(properties);
            cache.put(filePath, buildProperties);
        }
        return buildProperties;
    }

    public void dispose() {
        cache.clear();
    }

    protected static Properties readProperties(File propsFile) {
        Properties properties = new Properties();
        if (propsFile.canRead()) {
            // TODO should we fail the build if build.properties is missing?
            InputStream is = null;
            try {
                try {
                    is = new FileInputStream(propsFile);
                    properties.load(is);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return properties;
    }

    protected void interpolate(Properties properties, File baseDir) {
        MavenSession mavenSession = legacySupport.getSession();
        if (mavenSession == null) {
            return;
        }
        MavenProject mavenProject = MavenSessionUtils.getMavenProject(mavenSession, baseDir);
        // if no project for the given build.properties file could be found, no interpolation should happen
        if (mavenProject == null) {
            return;
        }
        Interpolator interpolator = interpolatorFactory.createInterpolator(mavenSession, mavenProject);

        for (Entry<Object, Object> entry : properties.entrySet()) {
            try {
                entry.setValue(interpolator.interpolate((String) entry.getValue()));
            } catch (InterpolationException e) {
                logger.warn("Unable to interpolate the build property value :" + entry.getValue());
            }
        }
    }

}
