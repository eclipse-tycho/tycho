/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.DefaultArtifactDescriptor;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.osgitools.targetplatform.MultiEnvironmentDependencyArtifacts;
import org.junit.Assert;
import org.junit.Test;

public class DefaultDependencyArtifactsTest {

    static IInstallableUnit unit(String id) {
        InstallableUnitDescription desc = new InstallableUnitDescription();
        desc.setId(id);
        desc.setVersion(Version.parseVersion("1.0.0"));
        return MetadataFactory.createInstallableUnit(desc);
    }

    @Test
    public void testVersionMatch() {
        String type = "foo";
        String id = "foo";

        DefaultDependencyArtifacts tp = new DefaultDependencyArtifacts();

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

    private void addArtifact(DefaultDependencyArtifacts tp, String type, String id, String version) {
        ArtifactKey key = new DefaultArtifactKey(type, id, version);
        tp.addArtifactFile(key, new File(version), null);
    }

    @Test
    public void testRelativePath() throws IOException {
        DefaultDependencyArtifacts tp = new DefaultDependencyArtifacts();

        File relative = new File("relative.xml");
        File canonical = new File("canonical.xml");

        tp.addArtifactFile(new DefaultArtifactKey("foo", "relative", "1"), relative, null);
        tp.addArtifactFile(new DefaultArtifactKey("foo", "canonical", "1"), canonical.getAbsoluteFile(), null);

        Assert.assertNotNull(tp.getArtifact(relative.getAbsoluteFile()));
        Assert.assertNotNull(getArtifactMapForLocation(canonical, tp));
    }

    @Test
    public void testEqualArtifacts() {
        DefaultDependencyArtifacts tp = new DefaultDependencyArtifacts();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, Set.of(unit("a")));
        tp.addArtifactFile(key, location, Set.of(unit("a")));

        Assert.assertEquals(1, tp.getArtifacts().size());
    }

    @Test
    public void testInconsistentArtifacts() {
        DefaultDependencyArtifacts tp = new DefaultDependencyArtifacts();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, Set.of(unit("a")));
        try {
            tp.addArtifactFile(key, location, Set.of(unit("a")));
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testMultiEnvironmentMetadataMerge() {
        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        DefaultDependencyArtifacts tpA = new DefaultDependencyArtifacts();
        tpA.addArtifactFile(key, location, Set.of(unit("a")));

        DefaultDependencyArtifacts tpB = new DefaultDependencyArtifacts();
        tpB.addArtifactFile(key, location, Set.of(unit("a"), unit("b")));

        MultiEnvironmentDependencyArtifacts tp = new MultiEnvironmentDependencyArtifacts(null);

        tp.addPlatform(new TargetEnvironment("a", "a", "a"), tpA);
        tp.addPlatform(new TargetEnvironment("b", "b", "b"), tpB);

        List<ArtifactDescriptor> artifacts = tp.getArtifacts();

        Assert.assertEquals(1, artifacts.size());

        Set<IInstallableUnit> units = artifacts.get(0).getInstallableUnits();
        Assert.assertEquals(2, units.size());
        Assert.assertTrue(units.contains(unit("a")));
        Assert.assertTrue(units.contains(unit("b")));
    }

    @Test
    public void testInstallableUnits() {
        DefaultDependencyArtifacts tp = new DefaultDependencyArtifacts();

        ArtifactKey key = new DefaultArtifactKey("type", "id", "version");
        File location = new File("location");

        tp.addArtifactFile(key, location, Set.of(unit("a")));
        tp.addNonReactorUnits(Set.of(unit("b")));

        Assert.assertEquals(Set.of(unit("a"), unit("b")), tp.getInstallableUnits());
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

        DefaultDependencyArtifacts tp1 = new DefaultDependencyArtifacts();
        tp1.addArtifact(new DefaultArtifactDescriptor(key, location, project, null, Set.of(unit("a"))));

        DefaultDependencyArtifacts tp2 = new DefaultDependencyArtifacts();
        tp2.addArtifact(new DefaultArtifactDescriptor(key, location, project, null, Set.of(unit("b"))));

        Assert.assertEquals(unit("a"), //
                getArtifactMapForLocation(location, tp1).get(null).getInstallableUnits().iterator().next());
        Assert.assertEquals(unit("b"), //
                getArtifactMapForLocation(location, tp2).get(null).getInstallableUnits().iterator().next());
    }

    private Map<String, ArtifactDescriptor> getArtifactMapForLocation(File location,
            DefaultDependencyArtifacts dependencyArtifacts) {
        Map<String, ArtifactDescriptor> map = dependencyArtifacts.getArtifact(location);
        assertNotNull("No artifacts found for location " + location.getAbsolutePath(), map);
        return map;
    }
}
