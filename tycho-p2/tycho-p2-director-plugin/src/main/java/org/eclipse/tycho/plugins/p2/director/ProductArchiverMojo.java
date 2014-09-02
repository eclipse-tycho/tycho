/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Sonatype Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.plugins.tar.TarGzArchiver;

/**
 * <p>
 * Creates archives with the product installations.
 * </p>
 */
@Mojo(name = "archive-products", defaultPhase = LifecyclePhase.PACKAGE)
public final class ProductArchiverMojo extends AbstractProductMojo {

    private static final String DEFAULT_ARCHIVE_FORMAT = "zip";
    private static final String TAR_GZ_ARCHIVE_FORMAT = "tar.gz";

    private abstract class ProductArchiver {
        abstract Archiver getArchiver() throws ArchiverException;
    }

    /**
     * Maps archive type to ProductArchiver
     */
    private final Map<String, ProductArchiver> productArchivers;

    /**
     * <p>
     * Maps os to format. By default a zip file will be created.
     * 
     * For example, the following configuration will create tar.gz product archives for Linux
     * 
     * <pre>
     * &lt;formats&gt;
     *   &lt;linux>tar.gz&lt;/linux&gt;
     * &lt;/formats&gt;
     * </pre>
     * 
     * Supported formats
     * 
     * <ul>
     * <li>zip</li>
     * <li>tar.gz</li>
     * </ul>
     * </p>
     */
    @Parameter
    private Map<String, String> formats;

    @Component(role = Archiver.class, hint = "zip")
    private Archiver zipArchiver;

    @Component(role = Archiver.class, hint = "tar")
    private TarArchiver tarArchiver;

    @Component
    private MavenProjectHelper helper;

    private static final boolean HAS_JAVA_NIO = checkForJavaNio();

    public ProductArchiverMojo() {
        productArchivers = new HashMap<String, ProductArchiver>();

        productArchivers.put("zip", new ProductArchiver() {
            @Override
            Archiver getArchiver() {
                return zipArchiver;
            }
        });

        productArchivers.put(TAR_GZ_ARCHIVE_FORMAT, new ProductArchiver() {
            @Override
            Archiver getArchiver() throws ArchiverException {
                TarArchiver.TarCompressionMethod tarCompressionMethod = new TarArchiver.TarCompressionMethod();
                tarCompressionMethod.setValue("gzip"); // surprisingly, compression names are private in plexus 
                tarArchiver.setCompression(tarCompressionMethod);
                // avoid lots of long file path (> 100 chars) warnings
                TarLongFileMode gnuMode = new TarLongFileMode();
                gnuMode.setValue(TarLongFileMode.GNU);
                tarArchiver.setLongfile(gnuMode);
                return tarArchiver;
            }
        });

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProductConfig config = getProductConfig();
        if (!config.uniqueAttachIds()) {
            throw new MojoFailureException("Artifact file names for the archived products are not unique. "
                    + "Configure the attachId or select a subset of products. Current configuration: "
                    + config.getProducts());
        }
        if (!HAS_JAVA_NIO) {
            warnThatTarGzWontHaveSymlinks(getEnvironments());
        }

        for (Product product : config.getProducts()) {
            for (TargetEnvironment env : getEnvironments()) {
                String format = getArchiveFormat(env);
                ProductArchiver productArchiver = productArchivers.get(format);
                if (productArchiver == null) {
                    throw new MojoExecutionException("Unknown or unsupported archive format os=" + env.getOs()
                            + " format=" + format);
                }

                File productArchive = new File(getProductsBuildDirectory(), getArchiveFileName(product) + "-"
                        + getOsWsArch(env, '.') + "." + format);

                try {
                    final File sourceDir = getProductMaterializeDirectory(product, env);
                    if (HAS_JAVA_NIO && TAR_GZ_ARCHIVE_FORMAT.equals(format)
                            && !"plexus".equals(getSession().getUserProperties().getProperty("tycho.tar"))) {
                        getLog().debug("Using commons-compress tar");
                        createCommonsCompressTarGz(productArchive, sourceDir);
                    } else {
                        Archiver archiver = productArchiver.getArchiver();
                        archiver.setDestFile(productArchive);
                        archiver.addDirectory(sourceDir);
                        archiver.createArchive();
                    }
                } catch (ArchiverException e) {
                    throw new MojoExecutionException("Error packing product", e);
                } catch (IOException e) {
                    throw new MojoExecutionException("Error packing product", e);
                }

                final String artifactClassifier = getArtifactClassifier(product, env);
                helper.attachArtifact(getProject(), format, artifactClassifier, productArchive);
            }
        }
    }

    private static boolean checkForJavaNio() {
        try {
            return Class.forName("java.nio.file.Files") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private void warnThatTarGzWontHaveSymlinks(List<TargetEnvironment> environments) {
        for (TargetEnvironment environment : environments) {
            if (TAR_GZ_ARCHIVE_FORMAT.equals(getArchiveFormat(environment))) {
                getLog().warn(
                        "JAVA_HOME is set to a JDK version older than 1.7. 'tar.gz' archives will not preserve symbolic links.");
                return;
            }
        }
    }

    private void createCommonsCompressTarGz(File productArchive, File sourceDir) throws IOException {
        TarGzArchiver archiver = new TarGzArchiver();
        archiver.setLog(getLog());
        archiver.addDirectory(sourceDir);
        archiver.setDestFile(productArchive);
        archiver.createArchive();
    }

    private String getArchiveFormat(TargetEnvironment env) {
        String format = formats != null ? formats.get(env.getOs()) : DEFAULT_ARCHIVE_FORMAT;
        if (format != null) {
            format = format.trim();
        }
        if (format == null || format.length() == 0) {
            format = DEFAULT_ARCHIVE_FORMAT;
        }
        return format;
    }

    static String getArchiveFileName(Product product) {
        if (product.getArchiveFileName() != null) {
            return product.getArchiveFileName();
        } else {
            return product.getId();
        }
    }

    static String getArtifactClassifier(Product product, TargetEnvironment environment) {
        // classifier (and hence artifact file name) ends with os.ws.arch (similar to Eclipse
        // download packages)
        final String artifactClassifier;
        if (product.getAttachId() == null) {
            artifactClassifier = getOsWsArch(environment, '.');
        } else {
            artifactClassifier = product.getAttachId() + "-" + getOsWsArch(environment, '.');
        }
        return artifactClassifier;
    }
}
