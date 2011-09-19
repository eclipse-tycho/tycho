/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.test;

import java.util.ArrayList;

import junit.framework.Assert;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultClasspathEntry;
import org.eclipse.tycho.surefire.TestFramework;

public class TestFrameworkTest extends PlexusTestCase {

    public void testJunit_v3_only() throws Exception {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT, "3.8.2"));

        Assert.assertEquals(TestFramework.TEST_JUNIT, new TestFramework().getTestFramework(cp));
    }

    public void testJunit_v4_only() throws Exception {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT, "4.5.0"));

        Assert.assertEquals(TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework(cp));
    }

    public void testJunit4_only() throws Exception {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT4, "4.5.0"));

        Assert.assertEquals(TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework(cp));
    }

    public void testJunit_v3_and_v4() throws Exception {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT, "3.8.2"));
        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT, "4.5.0"));

        Assert.assertEquals(TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework(cp));
    }

    public void testJunit_and_Junit4() throws Exception {
        ArrayList<ClasspathEntry> cp = new ArrayList<ClasspathEntry>();

        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT, "3.8.2"));
        cp.add(newDefaultClasspathEntry(TestFramework.TEST_JUNIT4, "4.5.0"));

        Assert.assertEquals(TestFramework.TEST_JUNIT4, new TestFramework().getTestFramework(cp));
    }

    private ClasspathEntry newDefaultClasspathEntry(String id, String version) {
        return new DefaultClasspathEntry(null, new DefaultArtifactKey(
                org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN, id, version), null, null);
    }
}
