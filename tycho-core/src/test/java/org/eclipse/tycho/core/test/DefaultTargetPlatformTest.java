/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.junit.Assert;
import org.junit.Test;

public class DefaultTargetPlatformTest {
    @Test
    public void testVersionMatch() {
        String type = "foo";
        String id = "foo";

        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        addArtifact(tp, type, id, "1.1.0");
        addArtifact(tp, type, id, "1.2.3");
        addArtifact(tp, type, id, "1.2.3.aaa");
        addArtifact(tp, type, id, "1.2.3.bbb");
        addArtifact(tp, type, id, "1.2.3.ccc");
        addArtifact(tp, type, id, "1.2.3.qualifier");
        addArtifact(tp, type, id, "1.2.3.zzz");

        addArtifact(tp, type, id, "5.6.7.zzz");

        // 0.0.0 or null match the latest version
        Assert.assertEquals("5.6.7.zzz", tp.getArtifact(type, id, null).getKey().getVersion());
        Assert.assertEquals("5.6.7.zzz", tp.getArtifact(type, id, "0.0.0").getKey().getVersion());

        // 1.2.3 matches the latest qualifier
        Assert.assertEquals("1.1.0", tp.getArtifact(type, id, "1.1.0").getKey().getVersion());
        Assert.assertEquals("1.2.3.zzz", tp.getArtifact(type, id, "1.2.3").getKey().getVersion());

        // 1.2.3.qualifier matches the latest qualifier
        Assert.assertEquals("1.1.0", tp.getArtifact(type, id, "1.1.0.qualifier").getKey().getVersion());
        Assert.assertEquals("1.2.3.zzz", tp.getArtifact(type, id, "1.2.3.qualifier").getKey().getVersion());

        // anything else matches just that exact version
        Assert.assertEquals("1.2.3.bbb", tp.getArtifact(type, id, "1.2.3.bbb").getKey().getVersion());

        // does not match anything
        Assert.assertNull(tp.getArtifact(type, id, "0.0.0.qualifier"));
        Assert.assertNull(tp.getArtifact(type, id, "1.0.0"));
        Assert.assertNull(tp.getArtifact(type, id, "1.0.0.qualifier"));
        Assert.assertNull(tp.getArtifact(type, id, "1.2.0"));
        Assert.assertNull(tp.getArtifact(type, id, "1.2.0.qualifier"));
        Assert.assertNull(tp.getArtifact(type, id, "9.9.9"));
        Assert.assertNull(tp.getArtifact(type, id, "9.9.9.qualifier"));
    }

    private void addArtifact(DefaultTargetPlatform tp, String type, String id, String version) {
        ArtifactKey key = new DefaultArtifactKey(type, id, version);
        tp.addArtifactFile(key, new File(version), null);
    }

    @Test
    public void testRelativePath() throws IOException {
        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        File relative = new File("relative.xml");
        File canonical = new File("canonical.xml");

        tp.addArtifactFile(new DefaultArtifactKey("foo", "relative", "1"), relative, null);
        tp.addArtifactFile(new DefaultArtifactKey("foo", "canonical", "1"), canonical.getCanonicalFile(), null);

        Assert.assertNotNull(tp.getArtifact(relative.getCanonicalFile()));
        Assert.assertNotNull(tp.getArtifact(canonical));
    }

    @Test
    public void testEqualArtifacts() {
        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, asSet("a"));
        tp.addArtifactFile(key, location, asSet("a"));

        Assert.assertEquals(1, tp.getArtifacts().size());
    }

    @Test
    public void testInconsistentArtifacts() {
        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, asSet("a"));
        try {
            tp.addArtifactFile(key, location, asSet("b"));
        } catch (IllegalStateException e) {
            // expected
        }

    }

    @Test
    public void testMultiEnvironmentMetadataMerge() {
        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        DefaultTargetPlatform tpA = new DefaultTargetPlatform();
        tpA.addArtifactFile(key, location, asSet("a"));

        DefaultTargetPlatform tpB = new DefaultTargetPlatform();
        tpB.addArtifactFile(key, location, asSet("a", "b"));

        MultiEnvironmentTargetPlatform tp = new MultiEnvironmentTargetPlatform();

        tp.addPlatform(new TargetEnvironment("a", "a", "a"), tpA);
        tp.addPlatform(new TargetEnvironment("b", "b", "b"), tpB);

        List<ArtifactDescriptor> artifacts = tp.getArtifacts();

        Assert.assertEquals(1, artifacts.size());

        Set<Object> units = artifacts.get(0).getInstallableUnits();
        Assert.assertEquals(2, units.size());
        Assert.assertTrue(units.contains("a"));
        Assert.assertTrue(units.contains("b"));
    }

    private Set<Object> asSet(Object... values) {
        Set<Object> result = new LinkedHashSet<Object>();
        for (Object v : values) {
            result.add(v);
        }
        return result;
    }

    @Test
    public void testInstallableUnits() {
        DefaultTargetPlatform tp = new DefaultTargetPlatform();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, asSet("a"));
        tp.addNonReactorUnits(asSet("b"));

        Assert.assertEquals(asSet("a", "b"), tp.getInstallableUnits());
    }

    @Test
    public void testDoNotCacheArtifactsThatRepresentReactorProjects() {
        // IInstallableUnit #hashCode and #equals methods only use (version,id) tuple to determine IU equality
        // Reactor projects are expected to produce different IUs potentially with the same (version,id) during the build
        // This test verifies that different DefaultTargetPlatform can have the same reactor project with different IUs
        // even when IUs (version,id) are the same

        ReactorProject project = new DefaultReactorProject(new MavenProject());
        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        DefaultTargetPlatform tp1 = new DefaultTargetPlatform();
        tp1.addArtifact(new DefaultArtifactDescriptor(key, location, project, null, asSet(new FunnyEquals("id", "a"))));

        DefaultTargetPlatform tp2 = new DefaultTargetPlatform();
        tp2.addArtifact(new DefaultArtifactDescriptor(key, location, project, null, asSet(new FunnyEquals("id", "b"))));

        Assert.assertEquals("a", //
                ((FunnyEquals) tp1.getArtifact(location).get(null).getInstallableUnits().iterator().next()).getData());
        Assert.assertEquals("b", //
                ((FunnyEquals) tp2.getArtifact(location).get(null).getInstallableUnits().iterator().next()).getData());
    }

    private static final class FunnyEquals {
        private final String id;
        private final String data;

        public FunnyEquals(String id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof FunnyEquals)) {
                return false;
            }

            FunnyEquals other = (FunnyEquals) obj;

            return id.equals(other.id);
        }

        public String getData() {
            return data;
        }
    }
}
