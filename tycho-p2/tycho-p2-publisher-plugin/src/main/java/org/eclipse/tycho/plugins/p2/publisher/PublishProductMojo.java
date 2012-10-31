/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.SingleResponseValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.buildversion.VersioningHelper;
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
 * @goal publish-products
 */
public final class PublishProductMojo extends AbstractPublishMojo {

    /**
     * <p>
     * The name of the p2 installation flavor to create. De facto, this parameter is set to
     * "tooling" in all uses of p2.
     * </p>
     * 
     * @deprecated This parameter has no useful effect and may be removed in a future version of
     *             Tycho.
     * @parameter default-value="tooling"
     */
    private String flavor;

    /** @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip" */
    private UnArchiver deflater;

    /** @component */
    private FileLockService fileLockService;

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        List<Object> productIUs = new ArrayList<Object>();
        for (File producFile : getEclipseRepositoryProject().getProductFiles(getProject())) {
            try {
                ProductConfiguration productConfiguration = ProductConfiguration.read(producFile);

                final Product buildProduct = prepareBuildProduct(producFile, productConfiguration, getBuildDirectory(),
                        getQualifier(), newInterpolator());

                Collection<?> ius = publisherService.publishProduct(buildProduct.productFile,
                        productConfiguration.includeLaunchers() ? getEquinoxExecutableFeature() : null, flavor);
                productIUs.addAll(ius);
            } catch (FacadeException e) {
                throw new MojoExecutionException("Exception while publishing product " + producFile.getAbsolutePath(),
                        e);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "I/O exception while writing product definition or copying launcher icons", e);
            }
        }
        return productIUs;
    }

    /**
     * Prepare the product file for the Eclipse publisher application.
     * <p>
     * Copies the product file and, if present, corresponding p2 advice file and other files
     * referenced in the .product file via relative path to a working directory. The folder is named
     * after the product ID (stored in the 'uid' attribute!), and the p2 advice file is renamed to
     * "p2.inf" so that the publisher application finds it.
     * </p>
     */
    static Product prepareBuildProduct(File productFile, ProductConfiguration productConfiguration,
            BuildOutputDirectory targetDir, String qualifier, Interpolator interpolator) throws MojoExecutionException,
            IOException {
        qualifyVersions(productConfiguration, qualifier);

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

        final String productId = productConfiguration.getId();
        if (productId == null) {
            throw new MojoExecutionException("The product file " + productFile.getName()
                    + " does not contain the mandatory attribute 'uid'");
        }

        File buildProductDir = targetDir.getChild("products/" + productId);
        buildProductDir.mkdirs();
        final Product buildProduct = new Product(new File(buildProductDir, productFile.getName()), new File(
                buildProductDir, "p2.inf"));
        ProductConfiguration.write(productConfiguration, buildProduct.productFile);
        copyP2Inf(getSourceP2InfFile(productFile), buildProduct.p2infFile);
        copyReferencedFiles(productConfiguration, productFile.getParentFile(), buildProductDir);
        return buildProduct;
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
     * Value class identifying a product file (and optionally an associated p2.inf file) for the
     * {@link PublishProductMojo}.
     */
    static class Product {
        private final File productFile;

        private final File p2infFile;

        public Product(File productFile) {
            this(productFile, getSourceP2InfFile(productFile));
        }

        public Product(File productFile, File p2infFile) {
            this.productFile = productFile;
            this.p2infFile = p2infFile;
        }

        public File getProductFile() {
            return productFile;
        }

        public File getP2infFile() {
            return p2infFile;
        }
    }

    /**
     * We expect an p2 advice file called "xx.p2.inf" next to a product file "xx.product".
     */
    static File getSourceP2InfFile(File productFile) {
        // This must match org.eclipse.tycho.p2.impl.publisher.ProductDependenciesAction.addPublisherAdvice(IPublisherInfo)
        final int indexOfExtension = productFile.getName().indexOf(".product");
        final String p2infFilename = productFile.getName().substring(0, indexOfExtension) + ".p2.inf";
        return new File(productFile.getParentFile(), p2infFilename);
    }

    static void qualifyVersions(ProductConfiguration productConfiguration, String buildQualifier) {
        // we need to expand the version otherwise the published artifact still has the '.qualifier'
        String productVersion = productConfiguration.getVersion();
        if (productVersion != null) {
            productVersion = replaceQualifier(productVersion, buildQualifier);
            productConfiguration.setVersion(productVersion);
        }

        // now same for the features and bundles that version would be something else than "0.0.0"
        for (FeatureRef featRef : productConfiguration.getFeatures()) {
            if (featRef.getVersion() != null && featRef.getVersion().indexOf(VersioningHelper.QUALIFIER) != -1) {
                String newVersion = replaceQualifier(featRef.getVersion(), buildQualifier);
                featRef.setVersion(newVersion);
            }
        }
        for (PluginRef plugRef : productConfiguration.getPlugins()) {
            if (plugRef.getVersion() != null && plugRef.getVersion().indexOf(VersioningHelper.QUALIFIER) != -1) {
                String newVersion = replaceQualifier(plugRef.getVersion(), buildQualifier);
                plugRef.setVersion(newVersion);
            }
        }
    }

    private static String replaceQualifier(final String productVersion, final String qualifier) {
        String replaceVersion = productVersion;
        if (productVersion.endsWith("." + VersioningHelper.QUALIFIER)) {
            int qualifierIndex = productVersion.length() - VersioningHelper.QUALIFIER.length();
            String unqualifiedVersion = productVersion.substring(0, qualifierIndex - 1);
            if (qualifier == null || "".equals(qualifier)) {
                replaceVersion = unqualifiedVersion;
            } else {
                replaceVersion = unqualifiedVersion + "." + qualifier;
            }
        }
        return replaceVersion;
    }

    /**
     * Same code than in the ProductExportMojo. Needed to get the launcher binaries.
     */
    // TODO implement at eclipse: have product publisher take the executables from the context repositories 
    private File getEquinoxExecutableFeature() throws MojoExecutionException, MojoFailureException {
        // TODO 364134 take the executable feature from the target platform instead
        DependencyArtifacts dependencyArtifacts = TychoProjectUtils.getDependencyArtifacts(getProject());
        ArtifactDescriptor artifact = dependencyArtifacts.getArtifact(ArtifactKey.TYPE_ECLIPSE_FEATURE,
                "org.eclipse.equinox.executable", null);

        if (artifact == null) {
            throw new MojoExecutionException("Unable to locate the equinox launcher feature (aka delta-pack)");
        }

        File equinoxExecFeature = artifact.getLocation();
        if (equinoxExecFeature.isDirectory()) {
            return equinoxExecFeature.getAbsoluteFile();
        } else {
            File unzipped = new File(getProject().getBuild().getOutputDirectory(), artifact.getKey().getId() + "-"
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

    protected Interpolator newInterpolator() {
        final MavenProject mavenProject = getProject();
        final MavenSession mavenSession = getSession();
        final Properties baseProps = new Properties();
        baseProps.putAll(mavenProject.getProperties());
        baseProps.putAll(mavenSession.getSystemProperties());
        baseProps.putAll(mavenSession.getUserProperties());

        final Settings settings = mavenSession.getSettings();

        // roughly match resources plugin behaviour

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new PrefixedObjectValueSource("project", mavenProject));
        interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        interpolator.addValueSource(new SingleResponseValueSource("localRepository", settings.getLocalRepository()));
        interpolator.addValueSource(new ValueSource() {
            public Object getValue(String expression) {
                return baseProps.getProperty(expression);
            }

            public void clearFeedback() {
            }

            @SuppressWarnings("rawtypes")
            public List getFeedback() {
                return Collections.EMPTY_LIST;
            }
        });
        return interpolator;
    }

}
