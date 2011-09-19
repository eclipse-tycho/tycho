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
package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.resolver.P2Resolver;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class P2DependencyGeneratorImplTest {
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_GROUP_ID = "org.eclipse.tycho.p2.impl.test";
    private P2GeneratorImpl subject;
    private LinkedHashSet<IInstallableUnit> units;
    private LinkedHashSet<IArtifactDescriptor> artifacts;

    @Before
    public void resetTestSubjectAndResultFields() {
        subject = new P2GeneratorImpl(true);

        units = new LinkedHashSet<IInstallableUnit>();
        artifacts = new LinkedHashSet<IArtifactDescriptor>();
    }

    private void generateDependencies(String testProjectId, String packagingType) throws IOException {
        File reactorProjectRoot = new File("resources/generator/" + testProjectId).getCanonicalFile();
        ArtifactMock reactorProject = new ArtifactMock(reactorProjectRoot, DEFAULT_GROUP_ID, testProjectId,
                DEFAULT_VERSION, packagingType);

        ArrayList<Map<String, String>> emptyEnvironments = new ArrayList<Map<String, String>>();

        subject.generateMetadata(reactorProject, emptyEnvironments, units, artifacts);
    }

    @Test
    public void bundle() throws Exception {
        generateDependencies("bundle", P2Resolver.TYPE_ECLIPSE_PLUGIN);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.bundle", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        assertEquals(2, unit.getRequirements().size());

        // not really necessary, but we get this because we reuse standard p2 implementation
        assertEquals(1, artifacts.size());
    }

    @Test
    public void feature() throws Exception {
        generateDependencies("feature", P2Resolver.TYPE_ECLIPSE_FEATURE);

        // no feature.jar IU because dependencyOnly=true
        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.feature.feature.group", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        assertEquals(4, unit.getRequirements().size());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void site() throws Exception {
        generateDependencies("site", P2Resolver.TYPE_ECLIPSE_UPDATE_SITE);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("site", unit.getId());
        assertEquals("raw:1.0.0.'SNAPSHOT'/format(n[.n=0;[.n=0;[-S]]]):1.0.0-SNAPSHOT", unit.getVersion().toString());
        assertEquals(1, unit.getRequirements().size());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void rcpBundle() throws Exception {
        generateDependencies("rcp-bundle", P2Resolver.TYPE_ECLIPSE_APPLICATION);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-bundle", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirement = new ArrayList<IRequirement>(unit.getRequirements());

        assertEquals(3, requirement.size());
        assertEquals("included.bundle", ((IRequiredCapability) requirement.get(0)).getName());

        // implicit dependencies because includeLaunchers="true"
        assertEquals("org.eclipse.equinox.executable.feature.group",
                ((IRequiredCapability) requirement.get(1)).getName());
        assertEquals("org.eclipse.equinox.launcher", ((IRequiredCapability) requirement.get(2)).getName());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void rcpFeature() throws Exception {
        generateDependencies("rcp-feature", P2Resolver.TYPE_ECLIPSE_APPLICATION);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-feature", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        assertEquals(3, unit.getRequirements().size());

        assertEquals(0, artifacts.size());
    }

    // TODO version ranges in feature, site and rcp apps
}
