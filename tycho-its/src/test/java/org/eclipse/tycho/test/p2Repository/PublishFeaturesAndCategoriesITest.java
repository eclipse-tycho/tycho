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
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.P2RepositoryTool.IU;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.Test;

public class PublishFeaturesAndCategoriesITest extends AbstractTychoIntegrationTest {

    private static final String QUALIFIER = "20101116-forcedQualifier";

    @Test
    public void testEclipseRepositoryWithIncludedFeatures() throws Exception {
        Verifier verifier = getVerifier("p2Repository.reactor", false);
        TargetDefinitionUtil.makeURLsAbsolute(new File(verifier.getBasedir(),
                "target-definition/prr.target-definition.target"), new File(
                "projects/p2Repository.reactor/target-definition"));

        /*
         * Do not execute "install" to ensure that features and bundles can be included directly
         * from the build results of the local reactor.
         */
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        P2RepositoryTool p2Repository = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir(),
                "eclipse-repository"));

        assertCategoryIU(p2Repository, QUALIFIER + ".example.category", "prr.example.feature.feature.group");

        assertFeatureIuAndArtifact(p2Repository, "prr.example.feature", "prr.example.included.feature.feature.group",
                "prr.example.bundle");
        assertBundleIuAndArtifact(p2Repository, "prr.example.bundle");
        assertBundleIuAndArtifact(p2Repository, "org.eclipse.core.contenttype", "3.4.1.R35x_v20090826-0451"); // a bundle from the target platform

        assertFeatureIuAndArtifact(p2Repository, "prr.example.included.feature", "prr.example.included.bundle");
        assertBundleIuAndArtifact(p2Repository, "prr.example.included.bundle");

    }

    private static void assertCategoryIU(P2RepositoryTool p2Repository, String categoryIuId, String featureIuId)
            throws Exception {
        IU categoryIU = p2Repository.getUniqueIU(categoryIuId);

        assertThat(categoryIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.category=true"));
        assertThat(categoryIU.getProperties(), hasItem("org.eclipse.equinox.p2.name=Example Category"));
        assertThat(categoryIU.getRequiredIds(), hasItem(featureIuId));
    }

    private static void assertFeatureIuAndArtifact(P2RepositoryTool p2Repository, String featureId,
            String... requiredIus) throws Exception {
        String featureIuId = featureId + ".feature.group";
        IU featureIU = p2Repository.getUniqueIU(featureIuId);

        assertThat(featureIU.getProperties(), hasItem("org.eclipse.equinox.p2.type.group=true"));
        assertThat(featureIU.getRequiredIds(), hasItems(requiredIus));

        File featureJar = p2Repository.getFeatureArtifact(featureId, "1.0.0." + QUALIFIER);
        assertThat(featureJar, isFile());
    }

    static private void assertBundleIuAndArtifact(P2RepositoryTool p2Repository, String bundleId) throws Exception {
        assertBundleIuAndArtifact(p2Repository, bundleId, "1.0.0." + QUALIFIER);
    }

    static private void assertBundleIuAndArtifact(P2RepositoryTool p2Repository, String bundleId, String version)
            throws Exception

    {
        assertThat(p2Repository.getAllUnitIds(), hasItem(bundleId));

        File bundleArtifact = p2Repository.getBundleArtifact(bundleId, version);
        assertThat(bundleArtifact, isFile());
    }

}
