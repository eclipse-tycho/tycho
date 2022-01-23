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
 *     Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products (gently sponsored by Compart AG)
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.maven.TychoInterpolator;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.resolver.shared.PlatformPropertiesUtils;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.osgi.framework.Version;

/**
 * <p>
 * Publishes all product definitions files (<tt>*.product</tt>) that are present in the root of the
 * project.
 * </p>
 * 
 * @see https://wiki.eclipse.org/Equinox/p2/Publisher
 */
@Mojo(name = "publish-products", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class PublishProductMojo extends AbstractPublishMojo {

    // as per https://download.eclipse.org/releases/luna/201502271000/features/org.eclipse.equinox.executable_3.6.102.v20150204-1316.jar
    private static final Version LUNA_SR2_EXECUTABLE_FEATURE_VERSION = Version.parseVersion("3.6.102.v20150204-1316");

    /**
     * <p>
     * The name of the p2 installation flavor to create. De facto, this parameter is set to
     * "tooling" in all uses of p2.
     * </p>
     * 
     * @deprecated This parameter has no useful effect and may be removed in a future version of
     *             Tycho.
     */
    @Parameter(defaultValue = "tooling")
    @Deprecated
    private String flavor;

    @Component(role = UnArchiver.class, hint = "zip")
    private UnArchiver deflater;

    @Component
    private FileLockService fileLockService;

    @Component(role = TychoProject.class, hint = PackagingType.TYPE_ECLIPSE_REPOSITORY)
    private EclipseRepositoryProject eclipseRepositoryProject;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        Interpolator interpolator = new TychoInterpolator(getSession(), getProject());
        PublishProductTool publisher = publisherServiceFactory.createProductPublisher(getReactorProject(),
                getEnvironments(), getQualifier(), interpolator);

        List<DependencySeed> seeds = new ArrayList<>();
        for (File productFile : eclipseRepositoryProject.getProductFiles(getReactorProject())) {
            try {
                ProductConfiguration productConfiguration = ProductConfiguration.read(productFile);
                if (productConfiguration.getId() == null || productConfiguration.getId().isEmpty()) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'uid'. Please ensure you entered an id in the product file.");
                } else if (productConfiguration.getVersion() == null || productConfiguration.getVersion().isEmpty()) {
                    throw new MojoExecutionException("The product file " + productFile.getName()
                            + " does not contain the mandatory attribute 'version'. Please ensure you entered a version in the product file.");
                }

                seeds.addAll(publisher.publishProduct(productFile,
                        productConfiguration.includeLaunchers() ? getExpandedLauncherBinaries() : null, flavor));
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "I/O exception while writing product definition or copying launcher icons", e);
            }
        }
        return seeds;
    }

    private File getExpandedLauncherBinaries() throws MojoExecutionException, MojoFailureException {
        // TODO 364134 take the executable feature from the target platform instead
        DependencyArtifacts dependencyArtifacts = TychoProjectUtils.getDependencyArtifacts(getReactorProject());
        ArtifactDescriptor artifact = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE,
                "org.eclipse.equinox.executable", null);

        if (artifact == null) {
            throw new MojoExecutionException(
                    "Unable to locate feature 'org.eclipse.equinox.executable'. This feature is required for native product launchers.");
        }
        checkMacOSLauncherCompatibility(artifact);
        File equinoxExecFeature = artifact.getLocation(true);
        if (equinoxExecFeature.isDirectory()) {
            return equinoxExecFeature.getAbsoluteFile();
        } else {
            File unzipped = new File(getProject().getBuild().getDirectory(),
                    artifact.getKey().getId() + "-" + artifact.getKey().getVersion());
            if (unzipped.exists()) {
                return unzipped.getAbsoluteFile();
            }
            try {
                FileLocker locker = fileLockService.getFileLocker(equinoxExecFeature);
                locker.lock();
                try {
                    // unzip now then:
                    unzipped.mkdirs();
                    deflater.setSourceFile(equinoxExecFeature);
                    deflater.setDestDirectory(unzipped);
                    deflater.extract();
                    return unzipped.getAbsoluteFile();
                } finally {
                    locker.release();
                }
            } catch (ArchiverException e) {
                throw new MojoFailureException("Unable to unzip the equinox executable feature", e);
            }
        }
    }

    private void checkMacOSLauncherCompatibility(ArtifactDescriptor executablesFeature) throws MojoExecutionException {
        if (!macOSConfigured()) {
            return;
        }
        Version featureVersion = Version.parseVersion(executablesFeature.getKey().getVersion());
        if (isLunaOrOlder(featureVersion)) {
            throw new MojoExecutionException(
                    "Detected Luna or older launcher feature org.eclipse.equinox.executable version " + featureVersion
                            + ".\n Native product launchers for MacOSX can only be built against Eclipse Mars or newer."
                            + "\nTo fix this, you can either build against Eclipse Mars or newer (recommended) or go back to Tycho <= 0.22.0");
        }
    }

    static boolean isLunaOrOlder(Version featureVersion) {
        return featureVersion.compareTo(LUNA_SR2_EXECUTABLE_FEATURE_VERSION) <= 0;
    }

    private boolean macOSConfigured() {
        for (TargetEnvironment env : getEnvironments()) {
            if (PlatformPropertiesUtils.OS_MACOSX.equals(env.getOs())) {
                return true;
            }
        }
        return false;
    }

}
