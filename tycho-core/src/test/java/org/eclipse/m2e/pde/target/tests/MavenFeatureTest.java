/*******************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests;

import java.util.List;

import org.eclipse.pde.core.target.ITargetLocation;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test that the generated features contain the expected content
 */
@RunWith(Parameterized.class)
public class MavenFeatureTest extends AbstractMavenTargetTest {
    @Parameter(0)
    public Boolean includeSource;

    @Parameters(name = "includeSource={0}")
    public static List<Boolean> dependencyConfigurations() {
        return List.of(false, true);
    }



    @Test
    public void testFeatureArtifact() throws Exception {
        // TODO: For real feature artifacts, which don't have a source-artifact, a
        // source feature is not generated (yet).
        Assume.assumeFalse(includeSource);
        ITargetLocation target = resolveMavenTarget(String.format(
                """
                        <location includeDependencyDepth="infinite" includeDependencyScopes="compile" includeSource="%s" missingManifest="error" type="Maven">
                        	<dependencies>
                        		<dependency>
                        			<groupId>org.eclipse.vorto</groupId>
                        			<artifactId>org.eclipse.vorto.feature</artifactId>
                        			<version>1.0.0</version>
                        			<type>jar</type>
                        		</dependency>
                        	</dependencies>
                        </location>
                        """,
                includeSource));
        assertStatusOk(target.getStatus());
        assertTargetBundles(target, List.of());
        List<ExpectedFeature> expectedFeature = List.of(generatedFeature("org.eclipse.vorto.feature", "1.0.0", List.of(//
                featurePlugin("org.eclipse.vorto.core", "1.0.0"), //
                featurePlugin("org.eclipse.vorto.editor", "1.0.0"), //
                featurePlugin("org.eclipse.vorto.editor.datatype", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.datatype.ide", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.datatype.ui", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.functionblock", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.functionblock.ide", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.functionblock.ui", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.infomodel", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.infomodel.ide", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.infomodel.ui", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.mapping", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.mapping.ide", "1.0.0"),
                featurePlugin("org.eclipse.vorto.editor.mapping.ui", "1.0.0"))));
        assertTargetFeatures(target, includeSource ? withSourceFeatures(expectedFeature) : expectedFeature);
    }

    private static ExpectedBundle junitPlatformCommons() {
        return originalOSGiBundle("junit-platform-commons", "1.9.3", "org.junit.platform:junit-platform-commons");
    }

    private static ExpectedBundle junitJupiterAPI() {
        return originalOSGiBundle("junit-jupiter-api", "5.9.3", "org.junit.jupiter:junit-jupiter-api");
    }

    private static ExpectedBundle apiGuardian() {
        return originalOSGiBundle("org.apiguardian.api", "1.1.2", "org.apiguardian:apiguardian-api");
    }

    private static ExpectedBundle opentest4j() {
        return originalOSGiBundle("org.opentest4j", "1.2.0", "org.opentest4j:opentest4j");
    }
}
