/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 Support setting the version for pomless builds
 *    Bachmann electronic GmbH. - #512326 Support product file names other than artifact id
 *    Guillaume Dufour - Support for release-process like Maven
 *    Bachmann electronic GmbH. - #517664 Support for updating p2iu versions
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import javax.inject.Inject;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.IU;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.eclipse.tycho.versions.bundle.MutableBndFile;
import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.utils.ProductFileFilter;
import org.osgi.framework.Constants;

/**
 * Update pom or Eclipse/OSGi version to make both versions consistent.
 */
public abstract class VersionUpdater {

    @Inject
    private Logger logger;

    @Inject
    private VersionsEngine engine;

    private static interface VersionAdaptor {
        String getVersion(ProjectMetadata project, Logger logger) throws IOException;
    }

    private static final Map<String, VersionAdaptor> updaters = new HashMap<>();

    private Collection<ProjectMetadata> projects;

    static {
        VersionAdaptor bundleVersionAdaptor = (project, logger) -> {
            File manifestFile = new File(project.getBasedir(), JarFile.MANIFEST_NAME);
            if (manifestFile.isFile()) {
                MutableBundleManifest manifest = MutableBundleManifest.read(manifestFile);
                return manifest.getVersion();
            }
            File bndFile = new File(project.getBasedir(), TychoConstants.PDE_BND);
            if (bndFile.isFile()) {
                MutableBndFile mutableBndFile = MutableBndFile.read(bndFile);
                return mutableBndFile.getValue(Constants.BUNDLE_VERSION);
            }
            throw new IllegalStateException("neither " + JarFile.MANIFEST_NAME + " nor " + TychoConstants.PDE_BND
                    + " file found in project " + project.getBasedir());
        };
        updaters.put(PackagingType.TYPE_ECLIPSE_PLUGIN, bundleVersionAdaptor);
        updaters.put(PackagingType.TYPE_ECLIPSE_TEST_PLUGIN, bundleVersionAdaptor);

        updaters.put(PackagingType.TYPE_ECLIPSE_FEATURE, (project, logger) -> {
            Feature feature = Feature.read(new File(project.getBasedir(), Feature.FEATURE_XML));
            return feature.getVersion();
        });

        VersionAdaptor productVersionAdapter = (project, logger) -> {
            PomFile pom = project.getMetadata(PomFile.class);
            File productFile = findProductFile(project, pom, logger);
            if (productFile == null) {
                return null;
            }
            ProductConfiguration product = ProductConfiguration.read(productFile);
            return product.getVersion();
        };
        updaters.put(PackagingType.TYPE_ECLIPSE_REPOSITORY, productVersionAdapter);
        updaters.put(PackagingType.TYPE_P2_IU, (project, logger) -> {
            IU iu = IU.loadIU(project.getBasedir());
            return iu.getVersion();
        });
    }

    public void setProjects(Collection<ProjectMetadata> projects) {
        this.projects = projects;
        engine.setProjects(projects);
    }

    public void apply() throws IOException {
        for (ProjectMetadata project : projects) {
            PomFile pom = project.getMetadata(PomFile.class);

            if (pom == null) {
                logger.info("Not a maven project " + project.getBasedir());
                continue;
            }

            String pomVersion = Versions.toCanonicalVersion(pom.getVersion());

            VersionAdaptor adaptor = updaters.get(pom.getPackaging());

            if (adaptor != null) {
                String osgiVersion = Versions.toCanonicalVersion(adaptor.getVersion(project, logger));

                if (osgiVersion != null && !Versions.isVersionEquals(pomVersion, osgiVersion)) {
                    addVersionChange(engine, pom, osgiVersion);
                }
            }
        }

        engine.apply();
    }

    protected abstract void addVersionChange(VersionsEngine engine, PomFile pom, String osgiVersion);

    private static File findProductFile(ProjectMetadata project, PomFile pom, Logger logger) {
        File productFile = new File(project.getBasedir(), pom.getArtifactId() + ".product");
        if (productFile.exists()) {
            return productFile;
        }
        File[] productFiles = project.getBasedir().listFiles(new ProductFileFilter());
        if (productFiles == null || productFiles.length == 0) {
            logger.warn("Skipping updating pom in directory " + project.getBasedir()
                    + " because no product file found to extract the (new) version");
            return null;
        }
        if (productFiles.length > 1) {
            logger.warn("Skipping updating pom in directory " + project.getBasedir()
                    + " because more than one product files have been found. Only one product file is supported or one must be named <artifactId>.product");
            return null;
        }
        return productFiles[0];
    }
}
