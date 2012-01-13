/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.ProductConfiguration;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.pom.MutablePomFile;

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
        String getVersion(ProjectMetadata project) throws IOException;
    }

    private static final Map<String, VersionAdaptor> updaters = new HashMap<String, PomVersionUpdater.VersionAdaptor>();

    private Collection<ProjectMetadata> projects;

    static {
        VersionAdaptor bundleVersionAdaptor = new VersionAdaptor() {
            public String getVersion(ProjectMetadata project) throws IOException {
                MutableBundleManifest manifest = MutableBundleManifest.read(new File(project.getBasedir(),
                        "META-INF/MANIFEST.MF"));
                return manifest.getVersion();
            }
        };
        updaters.put(ArtifactKey.TYPE_ECLIPSE_PLUGIN, bundleVersionAdaptor);
        updaters.put(ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN, bundleVersionAdaptor);

        updaters.put(ArtifactKey.TYPE_ECLIPSE_FEATURE, new VersionAdaptor() {
            public String getVersion(ProjectMetadata project) throws IOException {
                Feature feature = Feature.read(new File(project.getBasedir(), Feature.FEATURE_XML));
                return feature.getVersion();
            }
        });

        updaters.put(ArtifactKey.TYPE_ECLIPSE_APPLICATION, new VersionAdaptor() {
            public String getVersion(ProjectMetadata project) throws IOException {
                MutablePomFile pom = project.getMetadata(MutablePomFile.class);
                ProductConfiguration product = ProductConfiguration.read(new File(project.getBasedir(), pom
                        .getArtifactId() + ".product"));
                return product.getVersion();
            }
        });
    }

    public void setProjects(Collection<ProjectMetadata> projects) {
        this.projects = projects;
        engine.setProjects(projects);
    }

    public void apply() throws IOException {
        for (ProjectMetadata project : projects) {
            MutablePomFile pom = project.getMetadata(MutablePomFile.class);

            if (pom == null) {
                logger.info("Not a maven project " + project.getBasedir());
                continue;
            }

            String pomVersion = Versions.toCanonicalVersion(pom.getEffectiveVersion());

            VersionAdaptor adaptor = updaters.get(pom.getPackaging());

            if (adaptor != null) {
                String osgiVersion = Versions.toCanonicalVersion(adaptor.getVersion(project));

                if (!Versions.isVersionEquals(pomVersion, osgiVersion)) {
                    engine.addVersionChange(new VersionChange(pom, osgiVersion));
                }
            }
        }

        engine.apply();
    }
}
