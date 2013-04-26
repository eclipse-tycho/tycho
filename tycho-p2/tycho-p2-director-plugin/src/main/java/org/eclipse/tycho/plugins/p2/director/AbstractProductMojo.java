/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

abstract class AbstractProductMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter
     */
    private List<Product> products;
    // TODO only support a single product

    /** @parameter */
    private Map<String, ?> envSpecificConfiguration;
    /*
     * Note on above parameter: Plexus currently cannot parse the configuration we want here (see
     * http://bugs.eclipse.org/406688). As a workaround, we parse the from the DOM ourselves. In
     * order to get SiteDocs, we still mark this field as parameter and use the type Map. The
     * configuration we expect here parses as Map, but the keys will always be null.
     */

    EnvironmentSpecificConfigurations parsedEnvSpecificConfig;

    MavenProject getProject() {
        return project;
    }

    MavenSession getSession() {
        return session;
    }

    BuildOutputDirectory getBuildDirectory() {
        return new BuildOutputDirectory(getProject().getBuild().getDirectory());
    }

    File getProductsBuildDirectory() {
        return getBuildDirectory().getChild("products");
    }

    File getProductMaterializeDirectory(Product product, TargetEnvironment env) {
        return new File(getProductsBuildDirectory(), product.getId() + "/" + getOsWsArch(env, '/'));
    }

    List<TargetEnvironment> getEnvironments() {
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        return configuration.getEnvironments();
    }

    ProductConfig getProductConfig() throws MojoFailureException {
        return new ProductConfig(products, getProductsBuildDirectory());
    }

    void readEnvironmentSpecificConfiguration(EnvironmentSpecificConfiguration globalConfiguration)
            throws MojoExecutionException {
        if (envSpecificConfiguration != null) {
            Xpp3Dom rawConfiguration = RawConfigurationParserHelper.getRawMojoConfiguration(getProject(),
                    "tycho-p2-director-plugin", "envSpecificConfiguration");
            parsedEnvSpecificConfig = EnvironmentSpecificConfigurations.parse(globalConfiguration,
                    envSpecificConfiguration.keySet(), rawConfiguration);
        } else {
            parsedEnvSpecificConfig = EnvironmentSpecificConfigurations.globalOnly(globalConfiguration);
        }
    }

    static String getOsWsArch(TargetEnvironment env, char separator) {
        return env.getOs() + separator + env.getWs() + separator + env.getArch();
    }
}
