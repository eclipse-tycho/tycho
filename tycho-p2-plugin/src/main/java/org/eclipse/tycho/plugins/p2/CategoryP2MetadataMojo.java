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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.internal.repository.tools.XZCompressor;
import org.eclipse.tycho.p2tools.copiedfromp2.CategoryPublisherApplication;

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
    protected CategoryPublisherApplication getPublisherApplication(IProvisioningAgent agent) {
        return new CategoryPublisherApplication(agent);
    }

    @Override
    protected void addArguments(List<String> arguments) throws IOException, MalformedURLException {
        File location = getUpdateSiteLocation();
        arguments.add("-metadataRepository");
        arguments.add(location.toURI().toURL().toExternalForm());
        arguments.add("-categoryDefinition");
        arguments.add(categoryDefinition.toURI().toURL().toExternalForm());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            File location = getUpdateSiteLocation();
            File xmlFile = new File(location, "content.xml");
            File jarFile = new File(location, "content.jar");
            File xzFile = new File(location, "content.xml.xz");
            boolean jar = jarFile.isFile();
            boolean xz = xzFile.isFile();
            if (xmlFile.isFile()) {
                if (jar) {
                    jarFile.delete();
                }
            }
            if (xz) {
                xzFile.delete();
            }
            super.execute();
            try {
                if (jar && xmlFile.exists()) {
                    //need to recreate the jar
                    try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile))) {
                        jarOutputStream.putNextEntry(new JarEntry(xmlFile.getName()));
                        Files.copy(xmlFile.toPath(), jarOutputStream);
                    }
                }
                if (xz) {
                    //need to recreate the xz
                    XZCompressor xzCompressor = new XZCompressor();
                    xzCompressor.setPreserveOriginalFile(true);
                    xzCompressor.setRepoFolder(location.getAbsolutePath());
                    xzCompressor.compressRepo();
                }
            } catch (IOException e) {
                throw new MojoFailureException("compress content failed", e);
            }
        }
    }
}
