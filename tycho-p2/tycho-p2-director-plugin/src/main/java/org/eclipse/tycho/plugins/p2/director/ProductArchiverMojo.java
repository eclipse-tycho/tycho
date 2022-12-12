/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Sonatype Inc. - ongoing development
 *     Christoph Läubrich - Bug 568788 - Support new format .tgz in tycho-p2-director:archive-products
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.io.IOException;
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
import org.codehaus.plexus.archiver.bzip2.BZip2Archiver;
import org.codehaus.plexus.archiver.gzip.GZipArchiver;
import org.codehaus.plexus.archiver.snappy.SnappyArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarArchiver.TarCompressionMethod;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.xz.XZArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.plugins.tar.TarGzArchiver;

/**
 * Creates archives with the product installations.
 */
@Mojo(name = "archive-products", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class ProductArchiverMojo extends AbstractProductMojo {

    private static final String ZIP_ARCHIVE_FORMAT = "zip";
    private static final String TAR_GZ_ARCHIVE_FORMAT = "tar.gz";
    private static final String TGZ_ARCHIVE_FORMAT = "tgz";
    private static final String BZIP2_ARCHIVE_FORMAT = "bzip2";
    private static final String GZIP_ARCHIVE_FORMAT = "gzip";
    private static final String SNAPPY_ARCHIVE_FORMAT = "snappy";
    private static final String XZ_ARCHIVE_FORMAT = "xz";

    /**
     * Maps archive type to ProductArchiver
     */
    private static final Map<String, ThreadLocal<Archiver>> productArchivers;

    static {
        ThreadLocal<Archiver> zipFormat = ThreadLocal.withInitial(() -> new ZipArchiver());
        ThreadLocal<Archiver> tgzFormat = ThreadLocal.withInitial(() -> {
            TarArchiver tar = new TarArchiver();
            tar.setCompression(TarCompressionMethod.gzip);
            // avoid lots of long file path (> 100 chars) warnings
            tar.setLongfile(TarLongFileMode.gnu);
            return tar;
        });
        ThreadLocal<Archiver> bzip2Format = ThreadLocal.withInitial(() -> new BZip2Archiver());
        ThreadLocal<Archiver> gzipFormat = ThreadLocal.withInitial(() -> new GZipArchiver());
        ThreadLocal<Archiver> snappyFormat = ThreadLocal.withInitial(() -> new SnappyArchiver());
        ThreadLocal<Archiver> xzFormat = ThreadLocal.withInitial(() -> new XZArchiver());

        productArchivers = Map.of(ZIP_ARCHIVE_FORMAT, zipFormat, //
                TAR_GZ_ARCHIVE_FORMAT, tgzFormat, //
                TGZ_ARCHIVE_FORMAT, tgzFormat, //
                BZIP2_ARCHIVE_FORMAT, bzip2Format, //
                GZIP_ARCHIVE_FORMAT, gzipFormat, //
                SNAPPY_ARCHIVE_FORMAT, snappyFormat, //
                XZ_ARCHIVE_FORMAT, xzFormat //
        );

    }

    /**
     * <p>
     * Maps os to format. By default a zip file will be created for windows, and tar.gz for
     * linux/mac.
     * 
     * For example, the following configuration will create zip product archives for Linux
     * 
     * <pre>
     * {@code
     * <formats>
     *   <linux>zip</linux>
     * </formats>
     * }
     * </pre>
     * 
     * Supported formats
     * 
     * <ul>
     * <li>{@value #ZIP_ARCHIVE_FORMAT}</li>
     * <li>{@value #TAR_GZ_ARCHIVE_FORMAT}</li>
     * <li>{@value #TGZ_ARCHIVE_FORMAT}</li>
     * <li>{@value #BZIP2_ARCHIVE_FORMAT}</li>
     * <li>{@value #GZIP_ARCHIVE_FORMAT}</li>
     * <li>{@value #SNAPPY_ARCHIVE_FORMAT}</li>
     * <li>{@value #SNAPPY_ARCHIVE_FORMAT}</li>
     * <li>{@value #XZ_ARCHIVE_FORMAT}</li>
     * </ul>
     * </p>
     */
    @Parameter
    private Map<String, String> formats;

    @Component
    private MavenProjectHelper helper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProductConfig config = getProductConfig();
        if (!config.uniqueAttachIds()) {
            throw new MojoFailureException("Artifact file names for the archived products are not unique. "
                    + "Configure the attachId or select a subset of products. Current configuration: "
                    + config.getProducts());
        }

        for (Product product : config.getProducts()) {
            File bundlePool = getProductBundlePoolDirectory(product);
            if (bundlePool != null) {
                materialize(product, null);
            } else {
                for (TargetEnvironment env : getEnvironments()) {
                    materialize(product, env);
                }
            }
        }
    }

    private void materialize(Product product, TargetEnvironment env) throws MojoExecutionException {
        String format = getArchiveFormat(product, env);
        ThreadLocal<Archiver> productArchiver = productArchivers.get(format);
        if (productArchiver == null) {
            String os = env != null ? "os=" + env.getOs() : "";
            throw new MojoExecutionException("Unknown or unsupported archive format " + os + " format=" + format);
        }

        File productArchive = new File(getProductsBuildDirectory(),
                getArchiveFileName(product) + "-" + getOsWsArch(env, '.') + "." + format);

        try {
            final File sourceDir = getProductMaterializeDirectory(product, env);
            if ((TGZ_ARCHIVE_FORMAT.equals(format) || TAR_GZ_ARCHIVE_FORMAT.equals(format))
                    && !"plexus".equals(getSession().getUserProperties().getProperty("tycho.tar"))) {
                getLog().debug("Using commons-compress tar");
                createCommonsCompressTarGz(productArchive, sourceDir);
            } else {
                Archiver archiver = productArchiver.get();
                archiver.setDestFile(productArchive);
                DefaultFileSet fileSet = new DefaultFileSet(sourceDir);
                fileSet.setUsingDefaultExcludes(false);
                archiver.addFileSet(fileSet);
                archiver.createArchive();
            }
        } catch (ArchiverException | IOException e) {
            throw new MojoExecutionException("Error packing product", e);
        }

        final String artifactClassifier = getArtifactClassifier(product, env);
        helper.attachArtifact(getProject(), format, artifactClassifier, productArchive);
    }

    private void createCommonsCompressTarGz(File productArchive, File sourceDir) throws IOException {
        TarGzArchiver archiver = new TarGzArchiver();
        archiver.setLog(getLog());
        archiver.addDirectory(sourceDir);
        archiver.setDestFile(productArchive);
        archiver.createArchive();
    }

    private String getArchiveFormat(Product product, TargetEnvironment env) {
        if (formats != null) {
            String format;
            if (product.isMultiPlatformPackage()) {
                format = formats.get("multiPlatformPackage");
            } else {
                format = env != null ? formats.get(env.getOs()) : null;
            }
            if (format != null && !format.trim().isBlank()) {
                return format.trim();
            }
        }
        if (product.isMultiPlatformPackage()) {
            return ZIP_ARCHIVE_FORMAT;
        }
        if (env != null && ("linux".equals(env.getOs()) || "macosx".equals(env.getOs()))) {
            return TAR_GZ_ARCHIVE_FORMAT;
        }
        return ZIP_ARCHIVE_FORMAT;
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
