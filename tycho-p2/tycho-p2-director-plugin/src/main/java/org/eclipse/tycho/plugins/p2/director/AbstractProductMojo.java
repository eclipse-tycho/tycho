/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
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

    MavenProject getProject() {
        return project;
    }

    MavenSession getSession() {
        return session;
    }

    File getBuildDirectory() {
        return new File(getProject().getBuild().getDirectory());
    }

    File getProductsBuildDirectory() {
        return new File(getBuildDirectory(), "products");
    }

    File getProductMaterializeDirectory(Product product, TargetEnvironment env) {
        return new File(getProductsBuildDirectory(), product.getId() + "/" + getOsWsArch(env, '/'));
    }

    List<TargetEnvironment> getEnvironments() {
        TargetPlatformConfiguration configuration = TychoProjectUtils.getTargetPlatformConfiguration(project);
        return configuration.getEnvironments();
    }

    ProductConfig getProductConfig() throws MojoFailureException {
        return new ProductConfig(products, getProductsBuildDirectory(), getProject().getBasedir());
    }

    static String getOsWsArch(TargetEnvironment env, char separator) {
        return env.getOs() + separator + env.getWs() + separator + env.getArch();
    }
}
