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
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.repository.test.util.BuildPropertiesParserForTesting;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class P2DependencyGeneratorImplTest {
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_GROUP_ID = "org.eclipse.tycho.p2.impl.test";
    private static final String DEFAULT_CLASSIFIER = "classifier";
    private P2GeneratorImpl subject;
    private List<IInstallableUnit> units;
    private List<IArtifactDescriptor> artifacts;

    @Before
    public void resetTestSubjectAndResultFields() {
        subject = new P2GeneratorImpl(true);
        subject.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
    }

    private void generateDependencies(String testProjectId, String packagingType) throws IOException {
        File reactorProjectRoot = new File("resources/generator/" + testProjectId).getCanonicalFile();
        ArtifactMock reactorProject = new ArtifactMock(reactorProjectRoot, DEFAULT_GROUP_ID, testProjectId,
                DEFAULT_VERSION, packagingType, DEFAULT_CLASSIFIER);

        ArrayList<Map<String, String>> emptyEnvironments = new ArrayList<Map<String, String>>();

        DependencyMetadata metadata = subject.generateMetadata(reactorProject, emptyEnvironments);

        this.units = new ArrayList<IInstallableUnit>(metadata.getInstallableUnits());
        this.artifacts = new ArrayList<IArtifactDescriptor>(metadata.getArtifactDescriptors());
    }

    @Test
    public void bundle() throws Exception {
        generateDependencies("bundle", ArtifactKey.TYPE_ECLIPSE_PLUGIN);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.get(0);

        assertEquals("org.eclipse.tycho.p2.impl.test.bundle", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        assertEquals(2, unit.getRequirements().size());
        assertEquals(DEFAULT_CLASSIFIER, unit.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER));

        // not really necessary, but we get this because we reuse standard p2 implementation
        assertEquals(1, artifacts.size());
    }

    @Test
    public void bundle_with_p2_inf() throws Exception {
        generateDependencies("bundle-p2-inf", ArtifactKey.TYPE_ECLIPSE_PLUGIN);

        assertEquals(2, units.size());

        IInstallableUnit unit = units.get(0);
        assertEquals("org.eclipse.tycho.p2.impl.test.bundle-p2-inf", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirements = new ArrayList<IRequirement>(unit.getRequirements());
        assertEquals(1, requirements.size());
        IRequiredCapability requirement = (IRequiredCapability) requirements.get(0);
        assertEquals(IInstallableUnit.NAMESPACE_IU_ID, requirement.getNamespace());
        assertEquals("required.p2.inf", requirement.getName());

        assertEquals("iu.p2.inf", units.get(1).getId());
    }

    @Test
    public void feature() throws Exception {
        generateDependencies("feature", ArtifactKey.TYPE_ECLIPSE_FEATURE);

        // no feature.jar IU because dependencyOnly=true
        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.feature.feature.group", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        assertEquals(DEFAULT_CLASSIFIER, unit.getProperty(RepositoryLayoutHelper.PROP_CLASSIFIER));

        List<IRequirement> requirements = new ArrayList<IRequirement>(unit.getRequirements());
        assertEquals(6, requirements.size());

        IMatchExpression<IInstallableUnit> matches = requirements.get(4).getMatches();
        assertEquals(
                "providedCapabilities.exists(x | x.name == $0 && x.namespace == $1 && x.version >= $2 && x.version < $3)",
                matches.toString());
        assertEquals(Version.parseVersion("1.0.0"), matches.getParameters()[2]);
        assertEquals(Version.parseVersion("2.0.0"), matches.getParameters()[3]);

        assertEquals(0, artifacts.size());
    }

    @Test
    public void feature_with_p2_inf() throws Exception {
        generateDependencies("feature-p2-inf", ArtifactKey.TYPE_ECLIPSE_FEATURE);

        List<IInstallableUnit> units = new ArrayList<IInstallableUnit>(this.units);

        // no feature.jar IU because dependencyOnly=true
        assertEquals(2, units.size());

        IInstallableUnit unit = units.get(0);
        assertEquals("org.eclipse.tycho.p2.impl.test.feature-p2-inf.feature.group", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirements = new ArrayList<IRequirement>(unit.getRequirements());
        assertEquals(1, requirements.size());
        IRequiredCapability requirement = (IRequiredCapability) requirements.get(0);
        assertEquals(IInstallableUnit.NAMESPACE_IU_ID, requirement.getNamespace());
        assertEquals("required.p2.inf", requirement.getName());

        assertEquals(0, artifacts.size());

        assertEquals("iu.p2.inf", units.get(1).getId());
    }

    @Test
    public void site() throws Exception {
        generateDependencies("site", ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("site", unit.getId());
        assertEquals("raw:1.0.0.'SNAPSHOT'/format(n[.n=0;[.n=0;[-S]]]):1.0.0-SNAPSHOT", unit.getVersion().toString());
        assertEquals(1, unit.getRequirements().size());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void rcpBundle() throws Exception {
        generateDependencies("rcp-bundle", ArtifactKey.TYPE_ECLIPSE_APPLICATION);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-bundle", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirement = new ArrayList<IRequirement>(unit.getRequirements());

        assertEquals(3, requirement.size());
        assertEquals("included.bundle", ((IRequiredCapability) requirement.get(0)).getName());

        assertEquals("org.eclipse.equinox.launcher", ((IRequiredCapability) requirement.get(1)).getName());

        // implicit dependencies because includeLaunchers="true"
        assertEquals("org.eclipse.equinox.executable.feature.group",
                ((IRequiredCapability) requirement.get(2)).getName());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void rcp_with_p2_inf() throws Exception {
        generateDependencies("rcp-p2-inf", ArtifactKey.TYPE_ECLIPSE_APPLICATION);

        assertEquals(2, units.size());
        IInstallableUnit unit = units.get(0);

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-p2-inf", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirement = new ArrayList<IRequirement>(unit.getRequirements());

        assertEquals(2, requirement.size());
        assertEquals("org.eclipse.equinox.launcher", ((IRequiredCapability) requirement.get(0)).getName());
        assertEquals("required.p2.inf", ((IRequiredCapability) requirement.get(1)).getName());

        assertEquals(0, artifacts.size());

        assertEquals("iu.p2.inf", units.get(1).getId());
    }

    @Test
    public void rcpFeature() throws Exception {
        generateDependencies("rcp-feature", ArtifactKey.TYPE_ECLIPSE_APPLICATION);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-feature", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        assertEquals(3, unit.getRequirements().size());

        assertEquals(0, artifacts.size());
    }

    @Test
    public void rcpNoLaunchers() throws Exception {
        generateDependencies("rcp-no-launchers", ArtifactKey.TYPE_ECLIPSE_APPLICATION);

        assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        assertEquals("org.eclipse.tycho.p2.impl.test.rcp-no-launchers", unit.getId());
        assertEquals("1.0.0.qualifier", unit.getVersion().toString());

        List<IRequirement> requirement = new ArrayList<IRequirement>(unit.getRequirements());

        assertEquals(1, requirement.size());

        assertEquals("org.eclipse.equinox.launcher", ((IRequiredCapability) requirement.get(0)).getName());

        assertEquals(0, artifacts.size());
    }

    // TODO version ranges in feature, site and rcp apps
}
