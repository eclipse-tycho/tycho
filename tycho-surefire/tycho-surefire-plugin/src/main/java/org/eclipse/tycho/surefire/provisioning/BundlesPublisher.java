/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
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
package org.eclipse.tycho.surefire.provisioning;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAndBundlesPublisherApplication;

/**
 * Convenience wrapper around FeaturesAndBundlesPublisher to help with bundle jars not available
 * from a p2 repository.
 */
public class BundlesPublisher {

    private List<File> bundles = new ArrayList<>();
    private File workingDir;
    private Logger log;

    public BundlesPublisher(Logger log) {
        this.log = log;
    }

    public void addBundle(File bundle) {
        bundles.add(bundle);
    }

    /**
     * Creates a p2 repository in targetDirectory containing the bundles added.
     * 
     * @return URI of p2 repository created
     * @param targetDirectory
     *            (must be emtpy)
     * @throws Exception
     *             if an error occurs
     */
    public URI publishBundles(File targetDirectory) throws Exception {
        if (bundles.isEmpty()) {
            throw new MojoExecutionException("No bundles to be published");
        }
        File pluginsDir = new File(targetDirectory, "plugins");
        pluginsDir.mkdirs();
        for (File bundle : bundles) {
            FileUtils.copyFileToDirectory(bundle, pluginsDir);
        }
        log.info("Publishing " + bundles.size() + " bundles to " + targetDirectory);
        FeaturesAndBundlesPublisherApplication application = new FeaturesAndBundlesPublisherApplication();
        List<String> arguments = new ArrayList<String>();
        arguments.add("-artifactRepository");
        arguments.add(targetDirectory.toURI().toString());
        arguments.add("-metadataRepository");
        arguments.add(targetDirectory.toURI().toString());
        arguments.add("-compress");
        arguments.add("-publishArtifacts");
        arguments.add("-source");
        arguments.add(targetDirectory.toString());

        Object result = application.run(arguments.toArray(String[]::new));
        if (result != IApplication.EXIT_OK) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
        return targetDirectory.toURI();

    }

}
