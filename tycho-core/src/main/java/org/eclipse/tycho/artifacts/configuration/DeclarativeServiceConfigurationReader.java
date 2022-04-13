/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;
import org.osgi.framework.Version;

@Component(role = DeclarativeServiceConfigurationReader.class)
public class DeclarativeServiceConfigurationReader {

    public static final String DEFAULT_ADD_TO_CLASSPATH = "true";
    public static final String DEFAULT_DS_VERSION = "1.3";
    private static final String PROPERTY_CLASSPATH = "classpath";
    private static final String PROPERTY_DS_VERSION = "dsVersion";
    private static final String PROPERTY_ENABLED = "enabled";

    private static final String PDE_DS_ANNOTATIONS_PREFS = ".settings/org.eclipse.pde.ds.annotations.prefs";

    @Requirement
    private Logger logger;

    public DeclarativeServicesConfiguration getConfiguration(MavenProject mavenProject) throws IOException {
        Properties settings = getProjectSettings(mavenProject.getBasedir(), getMojoSettings(mavenProject, logger),
                mavenProject, logger);
        if (Boolean.parseBoolean(settings.getProperty(PROPERTY_ENABLED))) {
            return new DeclarativeServicesConfiguration() {

                @Override
                public boolean isAddToClasspath() {
                    return Boolean.parseBoolean(settings.getProperty(PROPERTY_CLASSPATH, DEFAULT_ADD_TO_CLASSPATH));
                }

                @Override
                public Version getSpecificationVersion() {
                    String property = settings.getProperty(PROPERTY_DS_VERSION, DEFAULT_DS_VERSION);
                    if (property.startsWith("V")) {
                        property = property.substring(1).replace('_', '.');
                    }
                    return Version.parseVersion(property);
                }
            };
        }
        return null;
    }

    private static Properties getProjectSettings(File basedir, Properties mojoProperties, MavenProject mavenProject,
            Logger logger) throws FileNotFoundException, IOException {
        Properties properties = new Properties(mojoProperties);
        File prefs = new File(basedir, PDE_DS_ANNOTATIONS_PREFS);
        if (prefs.exists()) {
            try (FileInputStream stream = new FileInputStream(prefs)) {
                properties.load(stream);
                logger.debug("declarative-services project configuration for " + mavenProject.toString() + ":"
                        + System.lineSeparator() + properties);
            }
        }
        return properties;
    }

    private static Properties getMojoSettings(MavenProject project, Logger logger) {
        Properties properties = new Properties();
        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-ds-plugin");
        if (plugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("declarative-services mojo configuration for " + project.toString() + ":"
                            + System.lineSeparator() + configuration.toString());
                }
                setProperty(properties, PROPERTY_CLASSPATH, configuration.getAttribute(PROPERTY_CLASSPATH));
                setProperty(properties, PROPERTY_DS_VERSION, configuration.getAttribute(PROPERTY_DS_VERSION));
                setProperty(properties, PROPERTY_ENABLED, configuration.getAttribute(PROPERTY_ENABLED));
            }
        }
        return properties;
    }

    private static void setProperty(Properties properties, String key, String attribute) {
        if (attribute != null && !attribute.isEmpty()) {
            properties.setProperty(key, attribute);
        }
    }
}
