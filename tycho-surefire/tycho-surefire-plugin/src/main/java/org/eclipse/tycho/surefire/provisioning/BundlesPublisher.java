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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.osgi.launching.equinox.P2ApplicationLauncher;

/**
 * Convenience wrapper around FeaturesAndBundlesPublisher to help with bundle jars not available
 * from a p2 repository.
 */
public class BundlesPublisher {

    private P2ApplicationLauncher launcher;
    private List<File> bundles = new ArrayList<>();
    private File workingDir;
    private int timeoutInSeconds = 300;
    private Logger log;

    public BundlesPublisher(P2ApplicationLauncher launcher, Logger log) {
        this.launcher = launcher;
        this.log = log;
    }

    public void addBundle(File bundle) {
        bundles.add(bundle);
    }

    public void setTimeout(int timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    /**
     * Creates a p2 repository in targetDirectory containing the bundles added.
     * 
     * @return URI of p2 repository created
     * @param targetDirectory
     *            (must be emtpy)
     */
    public URI publishBundles(File targetDirectory) throws IOException, MojoFailureException, MojoExecutionException {
        if (bundles.isEmpty()) {
            throw new MojoExecutionException("No bundles to be published");
        }
        File pluginsDir = new File(targetDirectory, "plugins");
        pluginsDir.mkdirs();
        for (File bundle : bundles) {
            FileUtils.copyFileToDirectory(bundle, pluginsDir);
        }
        log.info("Publishing " + bundles.size() + " bundles to " + targetDirectory);
        launcher.setWorkingDirectory(workingDir);
        launcher.setApplicationName("org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher");
        launcher.addArguments("-artifactRepository", targetDirectory.toURI().toString(), //
                "-metadataRepository", targetDirectory.toURI().toString(),//
                "-compress", //
                "-publishArtifacts",//
                "-source",//
                targetDirectory.toString());
        int result = launcher.execute(timeoutInSeconds);
        if (result != 0) {
            throw new MojoFailureException("P2 publisher return code was " + result);
        }
        return targetDirectory.toURI();

    }

}
