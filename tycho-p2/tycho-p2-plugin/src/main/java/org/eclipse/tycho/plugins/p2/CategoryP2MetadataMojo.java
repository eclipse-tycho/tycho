/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.equinox.launching.P2ApplicationLauncher;

/**
 * Adds category IUs to existing metadata repository.
 * https://help.eclipse.org/galileo/index.jsp?topic
 * =/org.eclipse.platform.doc.isv/guide/p2_publisher.html
 */
@Mojo(name = "category-p2-metadata", threadSafe = true)
public class CategoryP2MetadataMojo extends AbstractP2MetadataMojo {
    private static final Object LOCK = new Object();

    @Parameter(defaultValue = "${project.basedir}/category.xml")
    private File categoryDefinition;

    @Override
    protected String getPublisherApplication() {
        return "org.eclipse.equinox.p2.publisher.CategoryPublisher";
    }

    @Override
    protected void addArguments(P2ApplicationLauncher cli) throws IOException, MalformedURLException {
        cli.addArguments("-metadataRepository", getUpdateSiteLocation().toURL().toExternalForm(), //
                "-categoryDefinition", categoryDefinition.toURL().toExternalForm());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            super.execute();
        }
    }
}
