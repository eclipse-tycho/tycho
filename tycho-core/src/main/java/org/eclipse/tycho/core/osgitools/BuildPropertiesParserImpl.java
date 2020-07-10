/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.core.maven.TychoInterpolator;
import org.eclipse.tycho.core.shared.BuildProperties;
import org.eclipse.tycho.core.shared.BuildPropertiesImpl;
import org.eclipse.tycho.core.shared.BuildPropertiesParser;
import org.eclipse.tycho.core.utils.MavenSessionUtils;

@Component(role = BuildPropertiesParser.class)
public class BuildPropertiesParserImpl implements BuildPropertiesParser, Disposable {

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private Logger logger;

    private final Map<String, BuildPropertiesImpl> cache = new HashMap<>();

    public BuildPropertiesParserImpl() {
        // empty to let plexus create new instances
    }

    /**
     * Must only be used for tests!
     * 
     * @param legacySupport
     */
    protected BuildPropertiesParserImpl(LegacySupport legacySupport, Logger logger) {
        this.legacySupport = legacySupport;
        this.logger = logger;
    }

    @Override
    public BuildProperties parse(File baseDir) {
        File propsFile = new File(baseDir, BUILD_PROPERTIES);
        long lastModified = propsFile.lastModified();
        String filePath = propsFile.getAbsolutePath();
        BuildPropertiesImpl buildProperties = cache.get(filePath);
        if (buildProperties == null || lastModified > buildProperties.getTimestamp()) {
            Properties properties = readProperties(propsFile);
            interpolate(properties, baseDir);
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
        if (properties.isEmpty()) {
            return;
        }
        MavenSession mavenSession = legacySupport.getSession();
        if (mavenSession == null) {
            logger.warn("No maven session available, values in the build.properties will not be interpolated!");
            return;
        }
        // find the maven project for the currently used basedir, so that the correct maven project is used by the interpolator.
        // if no project could be found, it does not make sense to interpolate the properties
        MavenProject mavenProject = MavenSessionUtils.getMavenProject(mavenSession, baseDir);
        if (mavenProject == null) {
            logger.warn("No maven project found for baseDir '" + baseDir.getAbsolutePath()
                    + "', values in the build.properties will not be interpolated!");
            return;
        }
        TychoInterpolator interpolator = new TychoInterpolator(mavenSession, mavenProject);

        for (Entry<Object, Object> entry : properties.entrySet()) {
            entry.setValue(interpolator.interpolate((String) entry.getValue()));
        }
    }
}
