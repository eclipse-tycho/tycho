/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
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
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.buildversion.VersioningHelper;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.Launcher;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.ProductConfiguration.ConfigIni;
import org.eclipse.tycho.p2.tools.BuildOutputDirectory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;

/**
 * This goal invokes the product publisher for each product file found.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-products
 */
public final class PublishProductMojo extends AbstractPublishMojo {

    /**
     * @parameter default-value="tooling"
     */
    private String flavor;

    /**
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
    private UnArchiver deflater;

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        List<Object> productIUs = new ArrayList<Object>();
        for (Product product : getProducts()) {
            try {
                final Product buildProduct = prepareBuildProduct(product, getBuildDirectory(), getQualifier());

                Collection<?> ius = publisherService.publishProduct(buildProduct.productFile,
                        getEquinoxExecutableFeature(), flavor);
                productIUs.addAll(ius);
            } catch (FacadeException e) {
                throw new MojoExecutionException("Exception while publishing product "
                        + product.getProductFile().getAbsolutePath(), e);
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
    static Product prepareBuildProduct(Product product, BuildOutputDirectory targetDir, String qualifier)
            throws MojoExecutionException {
        try {
            ProductConfiguration productConfiguration = ProductConfiguration.read(product.productFile);

            qualifyVersions(productConfiguration, qualifier);

            final String productId = productConfiguration.getId();
            if (productId == null) {
                throw new MojoExecutionException("The product file " + product.productFile.getName()
                        + " does not contain the mandatory attribute 'uid'");
            }

            File buildProductDir = targetDir.getChild("products/" + productId);
            buildProductDir.mkdirs();
            final Product buildProduct = new Product(new File(buildProductDir, product.getProductFile().getName()),
                    new File(buildProductDir, "p2.inf"));
            ProductConfiguration.write(productConfiguration, buildProduct.productFile);
            copyP2Inf(product.p2infFile, buildProduct.p2infFile);
            copyReferencedFiles(productConfiguration, product.productFile.getParentFile(), buildProductDir);
            return buildProduct;
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "I/O exception while writing product definition or copying launcher icons", e);
        }
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

        /**
         * We expect an p2 advice file called "xx.p2.inf" next to a product file "xx.product".
         */
        static File getSourceP2InfFile(File productFile) {
            final int indexOfExtension = productFile.getName().indexOf(".product");
            final String p2infFilename = productFile.getName().substring(0, indexOfExtension) + ".p2.inf";
            return new File(productFile.getParentFile(), p2infFilename);
        }

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
        TargetPlatform targetPlatform = TychoProjectUtils.getTargetPlatform(getProject());
        ArtifactDescriptor artifact = targetPlatform.getArtifact(ArtifactKey.TYPE_ECLIPSE_FEATURE,
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
                // unzip now then:
                unzipped.mkdirs();
                deflater.setSourceFile(equinoxExecFeature);
                deflater.setDestDirectory(unzipped);
                deflater.extract();
                return unzipped.getAbsoluteFile();
            } catch (ArchiverException e) {
                throw new MojoFailureException("Unable to unzip the eqiuinox executable feature", e);
            }
        }
    }

    private List<Product> getProducts() {
        List<Product> result = new ArrayList<Product>();
        for (File productFile : getEclipseRepositoryProject().getProductFiles(getProject())) {
            result.add(new Product(productFile));
        }
        return result;
    }

}
