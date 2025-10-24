/*******************************************************************************
 * Copyright (c) 2015, 2021 Rapicorp, Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.TargetPlatform;
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
    private static ArtifactKey hamcrestInTP;

    private IUXmlTransformer subject;

    @BeforeClass
    public static void initTestResources() throws Exception {
        junit4InTP = new DefaultArtifactKey("eclipse-plugin", "org.junit4", "4.8.1.v20100302");
        hamcrestInTP = new DefaultArtifactKey("eclipse-plugin", "org.hamcrest.core", "1.1.0.v20090501071000");
    }

    @Test
    public void testExpandVersion() throws Exception {
        subject = new DefaultIUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        subject.replaceSelfQualifiers(iu, "1.0.0.ABC", "ABC");
		assertEquals("1.0.0.ABC", iu.getVersion());
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("demo.iu", "1.0.0.ABC")));
        assertThat(iu.getArtifacts(), hasItem(artifact("demo.iu", "1.0.0.ABC")));
    }

    @Test
    public void testExpandVersionInCapabilities() throws Exception {
        subject = new DefaultIUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        subject.replaceQualifierInCapabilities(iu.getProvidedCapabilites(), "CAPABILITY");
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("anotherId", "2.0.0.CAPABILITY")));
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("demo.iu", "1.0.0.CAPABILITY")));
        assertThat(iu.getProvidedCapabilites(), hasItem(capability("demo.iu", "1.1.1.CAPABILITY")));
    }

    @Test
    public void testExpandReferences() throws Exception {
        subject = new DefaultIUXmlTransformer();
        IU iu = IU.read(new File(TestUtil.getBasedir("projects/iuXmlValueReplacement/"), "p2iu.xml"));

        TargetPlatform tp = mock(TargetPlatform.class);
        when(tp.resolveArtifact("p2-installable-unit", "org.junit4", "0.0.0")).thenReturn(junit4InTP);
        when(tp.resolveArtifact("p2-installable-unit", "org.hamcrest.core", "1.1.0.qualifier"))
                .thenReturn(hamcrestInTP);

        subject.replaceZerosInRequirements(iu, tp);
        subject.replaceQualifierInRequirements(iu, tp);

        assertThat(iu.getRequiredCapabilites(), hasItem(requirement("org.junit4", "4.8.1.v20100302")));
        assertThat(iu.getRequiredCapabilites(), hasItem(requirement("org.hamcrest.core", "1.1.0.v20090501071000")));
    }

    private static Matcher<Element> requirement(final String id, final String version) {
		return new TypeSafeMatcher<>() {

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

    private static Matcher<Element> capability(final String id, final String version) {
		return new TypeSafeMatcher<>() {

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

    private static Matcher<Element> artifact(final String id, final String version) {
		return new TypeSafeMatcher<>() {

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
