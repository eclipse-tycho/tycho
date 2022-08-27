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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.equinox.internal.p2.updatesite.CategoryPublisherApplication;

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
    protected CategoryPublisherApplication getPublisherApplication() {
        return new CategoryPublisherApplication();
    }

    @Override
    protected void addArguments(List<String> arguments) throws IOException, MalformedURLException {
        arguments.add("-metadataRepository");
        arguments.add(getUpdateSiteLocation().toURL().toExternalForm());
        arguments.add("-categoryDefinition");
        arguments.add(categoryDefinition.toURL().toExternalForm());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            super.execute();
        }
    }
}
