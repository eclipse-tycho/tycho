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
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.launching.EquinoxInstallation;
import org.eclipse.sisu.equinox.launching.ProvisionedEquinoxInstallation;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

public class ProvisionedInstallationBuilder {

    private Logger log;
    private DirectorRuntime directorRuntime;

    private List<URI> metadataRepos = new ArrayList<>();
    private List<URI> artifactRepos = new ArrayList<>();
    private List<String> ius = new ArrayList<>();
    private File workingDir;
    private File destination;
    private TargetEnvironment targetEnvironment;
    private String profileName;
    private boolean installFeatures = true;

    private BundlesPublisher bundlesPublisher;
    private List<File> bundleJars = new ArrayList<>();

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public ProvisionedInstallationBuilder(DirectorRuntime directorRuntime, Logger log) {
        this.log = log;
        this.directorRuntime = directorRuntime;
        this.bundlesPublisher = new BundlesPublisher(log);
    }

    public void addMetadataRepositories(List<URI> uris) {
        metadataRepos.addAll(uris);
    }

    public void addArtifactRepositories(List<URI> uris) {
        artifactRepos.addAll(uris);
    }

    /**
     * Adds a plain bundle jar (not available in a p2 repository) to the IUs available during
     * install. The bundles added will be published into a temporary p2 repository prior to install.
     */
    public void addBundleJar(File bundleJar) {
        bundleJars.add(bundleJar);
    }

    public void addIUsToBeInstalled(List<String> ius) {
        this.ius.addAll(ius);
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }

    public void setTargetEnvironment(TargetEnvironment targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }

    public File getEffectiveDestination() {
        return DirectorRuntime.getDestination(destination, targetEnvironment);
    }

    public void setProfileName(String name) {
        this.profileName = name;
    }

    public void setInstallFeatures(boolean installFeatures) {
        this.installFeatures = installFeatures;
    }

    public EquinoxInstallation install() throws Exception {
        validate();
        publishPlainBundleJars();
        executeDirector();
        return new ProvisionedEquinoxInstallation(getEffectiveDestination());
    }

    private void publishPlainBundleJars() throws Exception {
        if (bundleJars.isEmpty()) {
            return;
        }
        for (File bundle : bundleJars) {
            bundlesPublisher.addBundle(bundle);
        }
        File bundlesRepoDir = new File(workingDir, "additionalBundles");
        if (bundlesRepoDir.isDirectory()) {
            FileUtils.deleteDirectory(bundlesRepoDir);
        }
        bundlesRepoDir.mkdirs();
        URI bundlesRepoURI = bundlesPublisher.publishBundles(bundlesRepoDir);
        metadataRepos.add(bundlesRepoURI);
        artifactRepos.add(bundlesRepoURI);
    }

    private void executeDirector() throws MojoFailureException {
        DirectorRuntime.Command command = directorRuntime.newInstallCommand(String.valueOf(targetEnvironment));
        command.addMetadataSources(metadataRepos);
        command.addArtifactSources(artifactRepos);
        for (String iu : ius) {
            command.addUnitToInstall(iu);
        }
        File effectiveDestination = getEffectiveDestination();
        command.setDestination(effectiveDestination);
        command.setProfileName(profileName);
        command.setInstallFeatures(installFeatures);
        command.setEnvironment(targetEnvironment);
        log.info("Installing IUs " + ius + " to " + effectiveDestination + " using " + command.getProfileProperties());
        try {
            command.execute();
        } catch (DirectorCommandException e) {
            throw new MojoFailureException("Installation of IUs " + ius + " failed", e);
        }
    }

    private void validate() {
        assertNotNull(workingDir, "workingDir");
        assertNotNull(destination, "destination");
        assertNotEmpty(metadataRepos, "metadataRepos");
        assertNotEmpty(artifactRepos, "artifactRepos");
        assertNotEmpty(ius, "ius");
    }

    private void assertNotEmpty(Collection<?> collection, String name) {
        if (collection.isEmpty()) {
            throw new IllegalStateException(name + " must not be empty");
        }
    }

    private void assertNotNull(Object object, String name) {
        if (object == null) {
            throw new IllegalStateException(name + " must not be null");
        }
    }

}
