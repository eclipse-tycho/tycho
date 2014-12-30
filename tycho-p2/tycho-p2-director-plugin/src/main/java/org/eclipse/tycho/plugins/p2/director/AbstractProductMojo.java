/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;

abstract class AbstractProductMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(property = "session", readonly = true)
    private MavenSession session;

    /**
     * <p>
     * Selection of products to be installed and configuration per product.
     * </p>
     * <p>
     * If the project contains more than one product file, you need to choose for which ones you
     * want to create distribution archives. If you choose to install more than one product, you
     * need to specify the <tt>attachId</tt> (which becomes a part of the classifier) to make the
     * classifiers unique. Example:
     * 
     * <pre>
     * &lt;plugin>
     *   &lt;groupId>org.eclipse.tycho&lt;/groupId>
     *   &lt;artifactId>tycho-p2-director-plugin&lt;/artifactId>
     *   &lt;version>${tycho-version}&lt;/version>
     *   &lt;executions>
     *     &lt;execution>
     *       &lt;id>create-distributions&lt;/id>
     *       &lt;goals>
     *         &lt;goal>materialize-products&lt;/goal>
     *         &lt;goal>archive-products&lt;/goal>
     *       &lt;/goals>
     *     &lt;/execution>
     *   &lt;/executions>
     *   &lt;configuration>
     *     &lt;products>
     *       &lt;product>
     *         &lt!-- select product with ID product.id; the archives get the classifiers "&lt;os>.&lt;ws>.&lt;arch>" -->
     *         &lt;id>product.id&lt;/id>
     *       &lt;/product>
     *       &lt;product>
     *         &lt!-- select product with ID other.product.id for the classifiers "other-&lt;os>.&lt;ws>.&lt;arch>" -->
     *         &lt;id>other.product.id&lt;/id>
     *         &lt;attachId>other&lt;/attachId>
     *       &lt;/product>
     *     &lt;/products>
     *   &lt;/configuration>
     * &lt;/plugin>
     * </pre>
     * 
     * The following snippet shows the optional parameters which can be specified per product:
     * 
     * <pre>
     *   &lt;configuration>
     *     &lt;products>
     *       &lt;product>
     *         &lt;id>product.id&lt;/id>
     *         &lt;!-- optional parameters -->
     *         &lt;rootFolder>&lt;/rootFolder>
     *         &lt;rootFolders>
     *           &lt;macosx>&lt;/macosx>
     *           &lt;linux>&lt;/linux>
     *           &lt;win32>&lt;/win32>
     *         &lt;/rootFolders>
     *       &lt;/product>
     *       ...
     *     &lt;/products>
     *   &lt;/configuration>
     * </pre>
     * 
     * Details on the product-specific configuration parameters:
     * <ul>
     * <li><tt>rootFolder</tt> - The path where the installed product shall be stored in the
     * archive, e.g. "eclipse". By default, the product is stored in the archive root.</li>
     * <li>
     * <tt>rootFolders</tt> - OS-specific installation root folders, overriding <tt>rootFolder</tt>.
     * Allowed children are <tt>&lt;macosx></tt>, <tt>&lt;win32></tt> and <tt>&lt;linux></tt> or any
     * other OS supported by p2. Since 0.18.0</li>
     * </ul>
     * 
     */
    @Parameter
    private List<Product> products;

    /**
     * Kill the forked process after a certain number of seconds. If set to 0, wait forever for the
     * process, never timing out.
     */
    @Parameter(property = "p2.timeout", defaultValue = "0")
    private int forkedProcessTimeoutInSeconds;

    int getForkedProcessTimeoutInSeconds() {
        return forkedProcessTimeoutInSeconds;
    }

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
        return new ProductConfig(products, TychoProjectUtils.getDependencySeeds(project));
    }

    static String getOsWsArch(TargetEnvironment env, char separator) {
        return env.getOs() + separator + env.getWs() + separator + env.getArch();
    }
}
