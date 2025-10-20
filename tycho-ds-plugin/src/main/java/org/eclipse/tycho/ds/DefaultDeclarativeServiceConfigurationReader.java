/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.ds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.DeclarativeServicesConfiguration;
import org.osgi.framework.Version;

@Named
@Singleton
public class DefaultDeclarativeServiceConfigurationReader implements DeclarativeServiceConfigurationReader {

    private static final String DS_PLUGIN = "org.eclipse.tycho:tycho-ds-plugin";
    private static final String PROPERTY_CLASSPATH = "classpath";
    private static final String PROPERTY_DS_VERSION = "dsVersion";
    private static final String PROPERTY_ENABLED = "enabled";
    private static final String PROPERTY_PATH = "path";

    private static final String PDE_DS_ANNOTATIONS_PREFS = ".settings/org.eclipse.pde.ds.annotations.prefs";

    private final Logger logger;

    @Inject
    public DefaultDeclarativeServiceConfigurationReader(Logger logger) {
        this.logger = logger;
    }

    @Override
    public DeclarativeServicesConfiguration getConfiguration(ReactorProject reactorProject) throws IOException {
        return getConfiguration(Objects.requireNonNull(reactorProject.adapt(MavenProject.class)));
    }

    @Override
    public DeclarativeServicesConfiguration getConfiguration(MavenProject mavenProject) throws IOException {
        Properties settings = getProjectSettings(mavenProject.getBasedir(), getMojoSettings(mavenProject, logger),
                mavenProject, logger);
		if (Boolean.parseBoolean(settings.getProperty(PROPERTY_ENABLED, DEFAULT_ENABLED))) {
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

                @Override
                public String getPath() {
                    return settings.getProperty(PROPERTY_PATH, DEFAULT_PATH);
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
        logger.debug("Project configuration for " + mavenProject + ": " + properties);
        return properties;
    }

    private static Properties getMojoSettings(MavenProject project, Logger logger) {
        Properties properties = new Properties();
        Plugin plugin = project.getPlugin(DS_PLUGIN);
        if (plugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("declarative-services mojo configuration for " + project.toString() + ":"
                            + System.lineSeparator() + configuration.toString());
                }
                setProperty(properties, PROPERTY_CLASSPATH, configuration.getChild(PROPERTY_CLASSPATH));
                setProperty(properties, PROPERTY_DS_VERSION, configuration.getChild(PROPERTY_DS_VERSION));
                setProperty(properties, PROPERTY_ENABLED, configuration.getChild(PROPERTY_ENABLED));
                setProperty(properties, PROPERTY_PATH, configuration.getChild(PROPERTY_PATH));
                logger.debug("Mojo configuration for " + project + ": " + properties);
            } else {
                logger.debug("DS Plugin " + DS_PLUGIN + " has no useable configuration.");
            }
        } else {
            logger.debug("DS Plugin " + DS_PLUGIN + " not found");
        }
        return properties;
    }

    private static void setProperty(Properties properties, String key, Xpp3Dom xpp3Dom) {
        if (xpp3Dom != null) {
            String value = xpp3Dom.getValue();
            if (value != null && !value.isBlank()) {
                properties.setProperty(key, value);
            }
        }
    }

}
