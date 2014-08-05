/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.artifacts.TargetPlatform;
import org.eclipse.tycho.model.IU;
import org.eclipse.tycho.testing.TestUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Test;

import de.pdark.decentxml.Element;

public class IUXmlTransformerTest {
    private static ArtifactKey junit4InTP;
    private static File junit4JarLocation;

    private IUXmlTransformer subject;

    @BeforeClass
    public static void initTestResources() throws Exception {
        junit4InTP = new DefaultArtifactKey("eclipse-plugin", "org.junit4", "4.8.1.v20100302");
        junit4JarLocation = TestUtil.getTestResourceLocation("eclipse/plugins/org.junit4_4.8.1.v20100302.jar");
    }

    @Test
    public void testExpandVersion() throws Exception {
        subject = new IUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        subject.replaceSelfQualifiers(iu, "1.0.0.ABC");
        assertEquals(iu.getVersion(), "1.0.0.ABC");
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("demo.iu", "1.0.0.ABC")));
        assertThat(iu.getArtifacts(), hasItem(artifact("demo.iu", "1.0.0.ABC")));
    }

    @Test
    public void testExpandVersionInCapabilities() throws Exception {
        subject = new IUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        subject.replaceQualifierInCapabilities(iu, "CAPABILITY");
        assertThat("1.0.0.CAPABILITY", is(not(iu.getVersion())));
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("anotherId", "2.0.0.CAPABILITY")));
        assertThat(iu.getProvidedCapabilites(), not(hasItem(capability("demo.iu", "1.0.0.CAPABILITY"))));
        assertThat(iu.getArtifacts(), not(hasItem(artifact("demo.iu", "1.0.0.CAPABILITY"))));
    }

    @Test
    public void testExpandReferences() throws Exception {
        subject = new IUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        TargetPlatform tp = mock(TargetPlatform.class);
        when(tp.resolveReference("p2-installable-unit", "org.junit4", "0.0.0")).thenReturn(junit4InTP);
        when(tp.getArtifactLocation(junit4InTP)).thenReturn(junit4JarLocation);

        subject.replaceZerosInRequirements(iu, tp);

        assertThat(iu.getRequiredCapabilites(), hasItem(requirement("org.junit4", "4.8.1.v20100302")));
    }

    private Matcher<Element> requirement(final String id, final String version) {
        return new TypeSafeMatcher<Element>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("requirement " + id + "/" + version);

            }

            @Override
            protected boolean matchesSafely(Element item) {
                return id.equals(item.getAttributeValue("name")) && version.equals(item.getAttributeValue("range"));
            }
        };
    }

    private Matcher<Element> capability(final String id, final String version) {
        return new TypeSafeMatcher<Element>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("capability " + id + "/" + version);

            }

            @Override
            protected boolean matchesSafely(Element item) {
                return id.equals(item.getAttributeValue("name")) && version.equals(item.getAttributeValue("version"));
            }
        };
    }

    private Matcher<Element> artifact(final String id, final String version) {
        return new TypeSafeMatcher<Element>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("artifact " + id + "/" + version);

            }

            @Override
            protected boolean matchesSafely(Element item) {
                return id.equals(item.getAttributeValue("id")) && version.equals(item.getAttributeValue("version"));
            }
        };
    }
}
