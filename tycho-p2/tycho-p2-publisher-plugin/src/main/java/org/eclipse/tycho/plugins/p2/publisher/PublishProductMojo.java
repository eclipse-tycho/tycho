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
package org.eclipse.tycho.plugins.p2.publisher;

import static org.apache.commons.lang3.StringUtils.isEmpty;

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
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.buildversion.VersioningHelper;
import org.eclipse.tycho.core.maven.InterpolationException;
import org.eclipse.tycho.core.maven.Interpolator;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.Launcher;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.ProductConfiguration.ConfigIni;
import org.eclipse.tycho.model.ProductConfiguration.ConfigurationProperty;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;

/**
 * <p>
 * Publishes all product definitions files (<tt>*.product</tt>) that are present in the root of the
 * project.
 * </p>
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 */
@Mojo(name = "publish-products", defaultPhase = LifecyclePhase.PACKAGE)
public final class PublishProductMojo extends AbstractPublishMojo {

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
    private String flavor;

    @Component(role = UnArchiver.class, hint = "zip")
    private UnArchiver deflater;

    @Component
    private FileLockService fileLockService;

    @Override
    protected Collection<DependencySeed> publishContent(PublisherService publisherService)
            throws MojoExecutionException, MojoFailureException {
        List<DependencySeed> result = new ArrayList<DependencySeed>();
        for (File producFile : getEclipseRepositoryProject().getProductFiles(getProject())) {
            try {
                ProductConfiguration productConfiguration = ProductConfiguration.read(producFile);
                if (isEmpty(productConfiguration.getId())) {
                    throw new MojoExecutionException("The product file " + producFile.getName()
                            + " does not contain the mandatory attribute 'uid'");
                } else if (isEmpty(productConfiguration.getVersion())) {
                    throw new MojoExecutionException("The product file " + producFile.getName()
                            + " does not contain the mandatory attribute 'version'");
                }
                expandProductVersionQualifier(productConfiguration, getQualifier());
                expandVersionsOfInclusions(producFile.getName(), productConfiguration,
                        TychoProjectUtils.getTargetPlatform(getProject()));
                Interpolator interpolator = new org.eclipse.tycho.core.maven.Interpolator(getSession(), getProject());
                interpolateProperties(productConfiguration, interpolator);
                extractRootFeatures(productConfiguration, result);

                final File preparedProductFile = writeProductForPublishing(producFile, productConfiguration,
                        getBuildDirectory());
                Collection<DependencySeed> seeds = publisherService.publishProduct(preparedProductFile,
                        productConfiguration.includeLaunchers() ? getEquinoxExecutableFeature() : null, flavor);
                result.addAll(seeds);
            } catch (FacadeException e) {
                throw new MojoExecutionException("Exception while publishing product " + producFile.getAbsolutePath(),
                        e);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "I/O exception while writing product definition or copying launcher icons", e);
            }
        }
        return result;
    }

    /**
     * Writes the product file and, if present, corresponding p2 advice file and other files
     * referenced in the .product file via relative path to a working directory. The folder is named
     * after the product ID (stored in the 'uid' attribute!), and the p2 advice file is renamed to
     * "p2.inf" so that the publisher application finds it.
     */
    // TODO since we call the product publisher in process (and no longer via the publisher application), writing to disk would no longer be needed...
    static File writeProductForPublishing(File originalProductFile, ProductConfiguration productConfiguration,
            BuildOutputDirectory targetDir) throws IOException {
        File buildProductDir = targetDir.getChild("products/" + productConfiguration.getId());
        buildProductDir.mkdirs();
        File preparedProductFile = new File(buildProductDir, originalProductFile.getName());
        ProductConfiguration.write(productConfiguration, preparedProductFile);
        copyP2Inf(getSourceP2InfFile(originalProductFile), new File(buildProductDir, "p2.inf"));
        copyReferencedFiles(productConfiguration, originalProductFile.getParentFile(), buildProductDir);
        return preparedProductFile;
    }

    private static void copyReferencedFiles(ProductConfiguration productConfiguration, File sourceDir, File targetDir)
            throws IOException {
        Launcher launcher = productConfiguration.getLauncher();
        List<String> relativePaths = new ArrayList<String>();
        if (launcher != null) {
            relativePaths.addAll(launcher.getLinuxIcon().values());
            relativePaths.addAll(launcher.getWindowsIcon().values());
            relativePaths.addAll(launcher.getSolarisIcon().values());
            relativePaths.addAll(launcher.getMacosxIcon().values());
        }
        ConfigIni configIni = productConfiguration.getConfigIni();
        if (configIni != null) {
            relativePaths.add(configIni.getLinuxConfigIni());
            relativePaths.add(configIni.getWin32ConfigIni());
            relativePaths.add(configIni.getSolarisConfigIni());
            relativePaths.add(configIni.getMacosxConfigIni());
        }
        copyFiles(sourceDir, targetDir, relativePaths);
    }

    private static void copyFiles(File sourceDir, File targetDir, List<String> relativePaths) throws IOException {
        for (String relativePath : relativePaths) {
            if (relativePath == null) {
                continue;
            }
            File sourceFile = new File(sourceDir, relativePath);
            if (sourceFile.isFile()) {
                FileUtils.copyFile(sourceFile, new File(targetDir, relativePath));
            }
        }
    }

    static void copyP2Inf(final File sourceP2Inf, final File buildP2Inf) throws IOException {
        if (sourceP2Inf.exists()) {
            FileUtils.copyFile(sourceP2Inf, buildP2Inf);
        }
    }

    /**
     * We expect an p2 advice file called "xx.p2.inf" next to a product file "xx.product".
     */
    static File getSourceP2InfFile(File productFile) {
        // This must match org.eclipse.tycho.p2.impl.publisher.ProductDependenciesAction.addPublisherAdvice(IPublisherInfo)
        final String productFileName = productFile.getName();
        final String p2infFilename = productFileName.substring(0, productFileName.length() - ".product".length())
                + ".p2.inf";
        return new File(productFile.getParentFile(), p2infFilename);
    }

    static void expandProductVersionQualifier(ProductConfiguration productConfiguration, String qualifier) {
        String unqualifiedVersion = stripQualifier(productConfiguration.getVersion());
        if (unqualifiedVersion != null) {
            if (qualifier == null || "".equals(qualifier)) {
                productConfiguration.setVersion(unqualifiedVersion);
            } else {
                productConfiguration.setVersion(unqualifiedVersion + "." + qualifier);
            }
        }
    }

    private static String stripQualifier(String version) {
        if (version.endsWith("." + VersioningHelper.QUALIFIER)) {
            int qualifierIndex = version.length() - VersioningHelper.QUALIFIER.length();
            return version.substring(0, qualifierIndex - 1);
        }
        // nothing to do
        return null;
    }

    static void expandVersionsOfInclusions(String productName, ProductConfiguration productConfiguration,
            TargetPlatform targetPlatform) throws MojoFailureException {

        for (FeatureRef featureRef : productConfiguration.getFeatures()) {
            try {
                ArtifactKey resolvedReference = targetPlatform.resolveReference(ArtifactType.TYPE_ECLIPSE_FEATURE,
                        featureRef.getId(), featureRef.getVersion());
                featureRef.setVersion(resolvedReference.getVersion());
            } catch (IllegalArtifactReferenceException e) {
                throw new MojoFailureException("Invalid reference to feature \"" + featureRef.getId()
                        + "\" with version \"" + featureRef.getVersion() + "\" in product " + productName + ": "
                        + e.getMessage(), e);
            }
        }

        for (PluginRef pluginRef : productConfiguration.getPlugins()) {
            try {
                ArtifactKey resolvedReference = targetPlatform.resolveReference(ArtifactType.TYPE_ECLIPSE_PLUGIN,
                        pluginRef.getId(), pluginRef.getVersion());
                pluginRef.setVersion(resolvedReference.getVersion());
            } catch (IllegalArtifactReferenceException e) {
                throw new MojoFailureException("Invalid reference to plug-in \"" + pluginRef.getId()
                        + "\" with version \"" + pluginRef.getVersion() + "\" in product " + productName + ": "
                        + e.getMessage(), e);
            }
        }
    }

    private static void interpolateProperties(ProductConfiguration productConfiguration, Interpolator interpolator)
            throws MojoExecutionException {
        List<ConfigurationProperty> properties = productConfiguration.getConfigurationProperties();
        if (properties != null && interpolator != null) {
            for (ConfigurationProperty property : properties) {
                try {
                    property.setValue(interpolator.interpolate(property.getValue()));
                } catch (InterpolationException e) {
                    throw new MojoExecutionException("Could not interpolate product configuration property "
                            + property.getName(), e);
                }
            }
        }
    }

    static void extractRootFeatures(ProductConfiguration product, List<DependencySeed> seeds) {
        final String productId = product.getId();

        // add root features as special dependency seed which are marked as "add-on" for the product
        DependencySeed.Filter filter = new DependencySeed.Filter() {
            @Override
            public boolean isAddOnFor(String type, String id) {
                return ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type) && productId.equals(id);
            }
        };
        for (FeatureRef feature : product.getFeatures()) {
            if (feature.getInstallMode() == FeatureRef.InstallMode.root) {
                // TODO 372780 get feature version from target platform that matches the specification; picking any version will no longer work once the the director installs from the target platform instead of from the resolved dependencies
                seeds.add(new DependencySeed(ArtifactType.TYPE_ECLIPSE_FEATURE, feature.getId(), null, filter));
            }
        }
        product.removeRootInstalledFeatures();
    }

    /**
     * Same code than in the ProductExportMojo. Needed to get the launcher binaries.
     */
    // TODO implement at eclipse: have product publisher take the executables from the context repositories 
    private File getEquinoxExecutableFeature() throws MojoExecutionException, MojoFailureException {
        // TODO 364134 take the executable feature from the target platform instead
        DependencyArtifacts dependencyArtifacts = TychoProjectUtils.getDependencyArtifacts(getProject());
        ArtifactDescriptor artifact = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE,
                "org.eclipse.equinox.executable", null);

        if (artifact == null) {
            throw new MojoExecutionException("Unable to locate the equinox launcher feature (aka delta-pack)");
        }

        File equinoxExecFeature = artifact.getLocation();
        if (equinoxExecFeature.isDirectory()) {
            return equinoxExecFeature.getAbsoluteFile();
        } else {
            File unzipped = new File(getProject().getBuild().getDirectory(), artifact.getKey().getId() + "-"
                    + artifact.getKey().getVersion());
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
                throw new MojoFailureException("Unable to unzip the eqiuinox executable feature", e);
            }
        }
    }

}
