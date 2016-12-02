/*******************************************************************************
 * Copyright (c) 2008, 2016 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Bachmann electronic GmbH. - #472579 - Support setting the version for pomless builds
 *******************************************************************************/
package org.eclipse.tycho.versions.pom.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.versions.pom.PomFile;
import org.junit.Assert;
import org.junit.Test;

public class MutablePomFileTest {

    private PomFile subject;

    @Test
    public void testWhitespacesInValuesAreIgnored() throws Exception {
        subject = getPom("/poms/whitespaceInElementText.xml");

        assertThat(subject.getParent().getGroupId(), is("ignorewhitespace"));
        assertThat(subject.getParent().getArtifactId(), is("parent"));
        assertThat(subject.getParent().getVersion(), is("1.0.0-SNAPSHOT"));
        assertThat(subject.getParentVersion(), is("1.0.0-SNAPSHOT"));
        assertThat(subject.getGroupId(), is("without.space"));
        assertThat(subject.getArtifactId(), is("bundle"));
        assertThat(subject.getVersion(), is("1.0.1-SNAPSHOT"));
        assertThat(subject.getPackaging(), is("pom"));

        assertThat(subject.getModules().get(0), is("child"));
        assertThat(subject.getProfiles().get(0).getModules().get(0), is("profileChild"));

        assertThat(subject.getProperties().get(0).getValue(), is("value-without-space"));
    }

    public void testSetVersion() throws Exception {
        subject = getPom("/poms/setVersion001.xml");
        subject.setVersion("1.2.3.qualifier");
        assertContent(subject, "/poms/setVersion001_expected.xml");
    }

    @Test
    public void testSetVersionOnPomWithProcessingInstruction() throws Exception {
        subject = getPom("/poms/setVersion002.xml");
        subject.setVersion("1.2.3.qualifier");
        assertContent(subject, "/poms/setVersion002_expected.xml");
    }

    @Test
    public void testSetExplicitVersion() throws Exception {
        subject = getPom("/poms/inheritedVersion.xml");
        assertThat(subject.getParentVersion(), is("1.0.2"));
        assertThat(subject.getVersion(), is("1.0.2"));

        subject.setVersion("1.1.0-SNAPSHOT");

        assertThat(subject.getVersion(), is("1.1.0-SNAPSHOT"));
        assertContent(subject, "/poms/inheritedVersion_changedProjectVersion.xml");
    }

    @Test
    public void testSetParentVersionDoesNotChangeEffectiveVersionOfChild() throws Exception {
        subject = getPom("/poms/inheritedVersion.xml");
        assertThat(subject.getParentVersion(), is("1.0.2"));
        assertThat(subject.getVersion(), is("1.0.2"));

        subject.setParentVersion("3.0.0");

        assertThat(subject.getParentVersion(), is("3.0.0"));
        assertThat(subject.getVersion(), is("1.0.2"));
        assertContent(subject, "/poms/inheritedVersion_changedParentVersion.xml");
    }

    @Test
    public void testSetVersionDoesNotIntroduceRedundantVersions() throws Exception {
        subject = getPom("/poms/inheritedVersion.xml");
        assertThat(subject.getParentVersion(), is("1.0.2"));
        assertThat(subject.getVersion(), is("1.0.2"));

        subject.setVersion("3.0.0");
        subject.setParentVersion("3.0.0");

        assertThat(subject.getVersion(), is("3.0.0"));
        assertThat(subject.getParentVersion(), is("3.0.0"));
        assertContent(subject, "/poms/inheritedVersion_changedBothVersions.xml");
    }

    @Test
    public void testSetVersionPreservesRedundantVersion() throws Exception {
        subject = getPom("/poms/inheritedVersionRedundant.xml");
        assertThat(subject.getParentVersion(), is("1.0.2"));
        assertThat(subject.getVersion(), is("1.0.2")); // project.version is stated explicitly in the POM, although it could be inherited

        subject.setVersion("3.0.0");
        subject.setParentVersion("3.0.0");

        assertThat(subject.getVersion(), is("3.0.0"));
        assertThat(subject.getParentVersion(), is("3.0.0"));
        assertContent(subject, "/poms/inheritedVersionRedundant_changedBothVersions.xml"); // project.version tag still exists
    }

    @Test
    public void testSetVersionPrefersNonRedundantVersionDeclarationIfVersionsWereDifferent() throws Exception {
        subject = getPom("/poms/inheritedVersion_changedProjectVersion.xml");
        assertThat(subject.getParentVersion(), is("1.0.2"));
        assertThat(subject.getVersion(), is("1.1.0-SNAPSHOT"));

        subject.setVersion("1.0.2");

        assertThat(subject.getVersion(), is("1.0.2"));
        assertContent(subject, "/poms/inheritedVersion.xml"); // no project.version tag
    }

    @Test
    public void testSetVersion_noFollowingSpaceCornerCase() throws Exception {
        subject = getPom("/poms/inheritedVersion_changedProjectVersionWithoutFollowingSpace.xml");
        subject.setVersion("1.0.2");
        assertContent(subject, "/poms/inheritedVersion.xml");
    }

    private PomFile getPom(String path) throws IOException {
        InputStream is = getClass().getResourceAsStream(path);
        try {
            return PomFile.read(is, true);
        } finally {
            IOUtil.close(is);
        }
    }

    private static void assertContent(PomFile pom, String path) throws IOException {
        byte[] expected = toByteArray(path);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PomFile.write(pom, buf);
        byte[] actual = buf.toByteArray();

        Assert.assertEquals(toAsciiString(expected), toAsciiString(actual));
    }

    private static byte[] toByteArray(String path) throws IOException {
        byte expected[];
        InputStream is = MutablePomFileTest.class.getResourceAsStream(path);
        try {
            expected = IOUtil.toByteArray(is);
        } finally {
            IOUtil.close(is);
        }
        return expected;
    }

    private static String toAsciiString(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes, "ascii");
    }

}
