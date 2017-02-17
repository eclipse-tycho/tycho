/*******************************************************************************
 * Copyright (c) 2011, 2017 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 Support setting the version for pomless builds
 *    Bachmann electronic GmbH. - #512326 Support product file names other than artifact id
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.pom.PomFile;
import org.eclipse.tycho.versions.utils.ProductFileFilter;

/**
 * Updates pom version to match Eclipse/OSGi metadata.
 */
@Component(role = PomVersionUpdater.class, instantiationStrategy = "per-lookup")
public class PomVersionUpdater {

    @Requirement
    private Logger logger;

    @Requirement
    private VersionsEngine engine;

    private static interface VersionAdaptor {
        String getVersion(ProjectMetadata project, Logger logger) throws IOException;
    }

    private static final Map<String, VersionAdaptor> updaters = new HashMap<>();

    private Collection<ProjectMetadata> projects;

    static {
        VersionAdaptor bundleVersionAdaptor = new VersionAdaptor() {
            @Override
            public String getVersion(ProjectMetadata project, Logger logger) throws IOException {
                MutableBundleManifest manifest = MutableBundleManifest
                        .read(new File(project.getBasedir(), "META-INF/MANIFEST.MF"));
                return manifest.getVersion();
            }
        };
        updaters.put(PackagingType.TYPE_ECLIPSE_PLUGIN, bundleVersionAdaptor);
        updaters.put(PackagingType.TYPE_ECLIPSE_TEST_PLUGIN, bundleVersionAdaptor);

        updaters.put(PackagingType.TYPE_ECLIPSE_FEATURE, new VersionAdaptor() {
            @Override
            public String getVersion(ProjectMetadata project, Logger logger) throws IOException {
                Feature feature = Feature.read(new File(project.getBasedir(), Feature.FEATURE_XML));
                return feature.getVersion();
            }
        });

        VersionAdaptor productVersionAdapter = new VersionAdaptor() {
            @Override
            public String getVersion(ProjectMetadata project, Logger logger) throws IOException {
                PomFile pom = project.getMetadata(PomFile.class);
                File productFile = findProductFile(project, pom, logger);
                if (productFile == null) {
                    return null;
                }
                ProductConfiguration product = ProductConfiguration.read(productFile);
                return product.getVersion();
            }
        };
        updaters.put(PackagingType.TYPE_ECLIPSE_APPLICATION, productVersionAdapter);
        updaters.put(PackagingType.TYPE_ECLIPSE_REPOSITORY, productVersionAdapter);
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
                    engine.addVersionChange(new VersionChange(pom, osgiVersion));
                }
            }
        }

        engine.apply();
    }

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
