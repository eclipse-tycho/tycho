/*******************************************************************************
 * Copyright (c) 2014, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.maven.plugin.testing.SilentLog;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.TargetPlatform;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.test.util.NoopFileLockService;
import org.eclipse.tycho.testing.TestUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Test;

public class FeatureXmlTransformerTest {
    private static ArtifactKey rcpFeatureInTP;
    private static ArtifactKey junit4InTP;
    private static File junit4JarLocation;

    private FeatureXmlTransformer subject;

    @BeforeClass
    public static void initTestResources() throws Exception {
        rcpFeatureInTP = new DefaultArtifactKey("eclipse-feature", "org.eclipse.rcp", "4.5.0.v20140918");
        junit4InTP = new DefaultArtifactKey("eclipse-plugin", "org.junit4", "4.8.1.v20100302");
        junit4JarLocation = TestUtil.getTestResourceLocation("eclipse/plugins/org.junit4_4.8.1.v20100302.jar");
    }

    @Test
    public void testExpandReferences() throws Exception {
        subject = new DefaultFeatureXmlTransformer(new SilentLog(), new NoopFileLockService());
        Feature feature = Feature
                .read(new File(TestUtil.getBasedir("projects/featureXmlVersionExpansion/"), "feature.xml"));

        TargetPlatform tp = mock(TargetPlatform.class);
        when(tp.resolveArtifact("eclipse-feature", "org.eclipse.rcp", "4.5.0.qualifier")).thenReturn(rcpFeatureInTP);
        when(tp.resolveArtifact("eclipse-plugin", "org.junit4", "4.8.1.qualifier")).thenReturn(junit4InTP);
        when(tp.getArtifactLocation(junit4InTP)).thenReturn(junit4JarLocation);

        subject.expandReferences(feature, tp);

        assertThat(feature.getIncludedFeatures(), hasItem(feature("org.eclipse.rcp", "4.5.0.v20140918")));

        assertThat(feature.getPlugins(), hasItem(plugin("org.junit4", "4.8.1.v20100302")));
        PluginRef plugin = feature.getPlugins().get(0);
		assertEquals("org.junit4", plugin.getId());
    }

    private static Matcher<FeatureRef> feature(final String id, final String version) {
		return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("feature ref to " + id + "_" + version);

            }

            @Override
            protected boolean matchesSafely(FeatureRef item) {
                return id.equals(item.getId()) && version.equals(item.getVersion());
            }
        };
    }

    private static Matcher<PluginRef> plugin(final String id, final String version) {
		return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("plugin ref to " + id + "_" + version);
            }

            @Override
            protected boolean matchesSafely(PluginRef item) {
                return id.equals(item.getId()) && version.equals(item.getVersion());
            }
        };
    }

}
