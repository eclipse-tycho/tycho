/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.tycho.BuildDirectory;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
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
     * &lt;plugin&gt;
     *   &lt;groupId&gt;org.eclipse.tycho&lt;/groupId&gt;
     *   &lt;artifactId&gt;tycho-p2-director-plugin&lt;/artifactId&gt;
     *   &lt;version&gt;${tycho-version}&lt;/version&gt;
     *   &lt;executions&gt;
     *     &lt;execution&gt;
     *       &lt;id&gt;create-distributions&lt;/id&gt;
     *       &lt;goals&gt;
     *         &lt;goal&gt;materialize-products&lt;/goal&gt;
     *         &lt;goal&gt;archive-products&lt;/goal&gt;
     *       &lt;/goals&gt;
     *     &lt;/execution&gt;
     *   &lt;/executions&gt;
     *   &lt;configuration&gt;
     *     &lt;products&gt;
     *       &lt;product&gt;
     *         &lt!-- select product with ID product.id; the archives get the classifiers "&lt;os&gt;.&lt;ws&gt;.&lt;arch&gt;" -->
     *         &lt;id&gt;product.id&lt;/id&gt;
     *       &lt;/product&gt;
     *       &lt;product&gt;
     *         &lt!-- select product with ID other.product.id for the classifiers "other-&lt;os&gt;.&lt;ws&gt;.&lt;arch&gt;" -->
     *         &lt;id&gt;other.product.id&lt;/id&gt;
     *         &lt;attachId&gt;other&lt;/attachId&gt;
     *       &lt;/product&gt;
     *     &lt;/products&gt;
     *   &lt;/configuration&gt;
     * &lt;/plugin&gt;
     * </pre>
     * 
     * The following snippet shows the optional parameters which can be specified per product:
     * 
     * <pre>
     *   &lt;configuration&gt;
     *     &lt;products&gt;
     *       &lt;product&gt;
     *         &lt;id&gt;product.id&lt;/id&gt;
     *         &lt;!-- optional parameters -->
     *         &lt;rootFolder&gt;&lt;/rootFolder&gt;
     *         &lt;rootFolders&gt;
     *           &lt;macosx&gt;&lt;/macosx&gt;
     *           &lt;linux&gt;&lt;/linux&gt;
     *           &lt;freebsd&gt;&lt;/freebsd&gt;
     *           &lt;win32&gt;&lt;/win32&gt;
     *         &lt;/rootFolders&gt;
     *       &lt;/product&gt;
     *       ...
     *     &lt;/products&gt;
     *   &lt;/configuration&gt;
     * </pre>
     * 
     * Details on the product-specific configuration parameters:
     * <ul>
     * <li><tt>rootFolder</tt> - The path where the installed product shall be stored in the
     * archive, e.g. "eclipse". By default, the product is stored in the archive root.</li>
     * <li><tt>rootFolders</tt> - OS-specific installation root folders, overriding
     * <tt>rootFolder</tt>. Allowed children are <tt>&lt;macosx&gt;</tt>, <tt>&lt;win32&gt;</tt>,
     * <tt>&lt;linux&gt;</tt> and <tt>&lt;freebsd&gt;</tt> or any other OS supported by p2. Since
     * 0.18.0</li>
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

    BuildDirectory getBuildDirectory() {
        return DefaultReactorProject.adapt(project).getBuildDirectory();
    }

    File getProductsBuildDirectory() {
        return getBuildDirectory().getChild("products");
    }

    File getProductMaterializeDirectory(Product product, TargetEnvironment env) {
        return new File(getProductsBuildDirectory(), product.getId() + "/" + getOsWsArch(env, '/'));
    }

    List<TargetEnvironment> getEnvironments() {
        TargetPlatformConfiguration configuration = TychoProjectUtils
                .getTargetPlatformConfiguration(DefaultReactorProject.adapt(project));
        return configuration.getEnvironments();
    }

    ProductConfig getProductConfig() throws MojoFailureException {
        return new ProductConfig(products, TychoProjectUtils.getDependencySeeds(DefaultReactorProject.adapt(project)));
    }

    static String getOsWsArch(TargetEnvironment env, char separator) {
        return env.getOs() + separator + env.getWs() + separator + env.getArch();
    }
}
