/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.TargetEnvironment;
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

        Collection<IInstallableUnit> units = artifacts.get(0).getInstallableUnits();
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

}
