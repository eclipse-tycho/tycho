/*******************************************************************************
 * Copyright (c) 2008, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - apply DRY principle
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_REPOSITORY;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE;
import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.customEEResolutionHintProvider;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.standardEEResolutionHintProvider;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.ReactorProjectStub;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.target.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.target.P2TargetPlatform;
import org.eclipse.tycho.p2.target.ee.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class P2ResolverTest extends P2ResolverTestBase {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    ReactorProject projectToResolve;

    @Before
    public void initDefaultResolver() throws Exception {
//        org.eclipse.equinox.internal.p2.core.helpers.Tracing.DEBUG_PLANNER_PROJECTOR = true;
        pomDependencies = resolverFactory.newPomDependencyCollectorImpl();
        impl = new P2ResolverImpl(tpFactory, logVerifier.getLogger());
        impl.setEnvironments(getEnvironments());
    }

    @Test
    public void testBasic() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Test
    public void testSiteConflictingDependenciesResolver() throws IOException {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        File[] projects = new File[] { resourceFile("siteresolver/bundle342"), //
                resourceFile("siteresolver/bundle352"), //
                resourceFile("siteresolver/feature342"), // 
                resourceFile("siteresolver/feature352") };

        addContextProject(projects[0], TYPE_ECLIPSE_PLUGIN);
        addContextProject(projects[1], TYPE_ECLIPSE_PLUGIN);
        addContextProject(projects[2], TYPE_ECLIPSE_FEATURE);
        addContextProject(projects[3], TYPE_ECLIPSE_FEATURE);

        projectToResolve = createReactorProject(resourceFile("siteresolver/site"), TYPE_ECLIPSE_UPDATE_SITE, "site");

        P2ResolutionResult result = impl.collectProjectDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(projects.length, result.getArtifacts().size());
        for (File project : projects) {
            assertContainLocation(result, project);
        }
        // TODO the eclipse-update-site module itself is not in the resolution result; is this needed?

        // conflicting dependency mode only collects included artifacts - the referenced non-reactor unit
        // org.eclipse.osgi is not included
        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), not(hasItem(unitWithId("org.eclipse.osgi"))));
    }

    private static void assertContainLocation(P2ResolutionResult result, File location) {
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            if (entry.getLocation().equals(location)) {
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void testDuplicateInstallableUnit() throws Exception {
        projectToResolve = createReactorProject(resourceFile("duplicate-iu/featureA"), TYPE_ECLIPSE_FEATURE, "featureA");
        reactorProjects.add(createReactorProject(resourceFile("duplicate-iu/featureA2"), TYPE_ECLIPSE_FEATURE,
                "featureA2"));

        // TODO 353889 make the duplicate detection work without having the current project IUs in the target platform
        reactorProjects.add(projectToResolve);

        try {
            impl.resolveDependencies(getTargetPlatform(), projectToResolve);
            fail();
        } catch (DuplicateReactorIUsException e) {
            // TODO proper assertion
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testProjectToResolveOverwritesTargetContent() throws Exception {
        reactorProjects.add(createReactorProject(resourceFile("resolver/bundle.optional-dep"), TYPE_ECLIPSE_PLUGIN,
                "bundle.optional-dep", OptionalResolutionAction.IGNORE));
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.optional-dep"), TYPE_ECLIPSE_PLUGIN,
                "bundle.optional-dep", OptionalResolutionAction.REQUIRE);

        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());
        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
        // the unit from projectToResolve with the dependency has been used
    }

    @Test
    public void testFeatureInstallableUnits() throws Exception {
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        projectToResolve = createReactorProject(resourceFile("resolver/feature01"), TYPE_ECLIPSE_FEATURE, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());
        Assert.assertEquals(1, result.getArtifacts().iterator().next().getInstallableUnits().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }

    @Test
    public void testSourceBundle() throws Exception {
        String featureId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        projectToResolve = createReactorProject(resourceFile("sourcebundles/feature01"), TYPE_ECLIPSE_FEATURE,
                featureId);

        File bundle = resourceFile("sourcebundles/bundle01");
        String bundleId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String bundleVersion = "1.0.0-SNAPSHOT";
        reactorProjects.add(createReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, bundleId));

        ReactorProjectStub sb = new ReactorProjectStub(bundle, bundleId, bundleId, bundleVersion, TYPE_ECLIPSE_PLUGIN);
        DependencyMetadata metadata = new SourcesBundleDependencyMetadataGenerator().generateMetadata(new ArtifactMock(
                sb, "source"), getEnvironments(), null);
        sb.setDependencyMetadata(metadata);
        reactorProjects.add(sb);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size());
        List<P2ResolutionResult.Entry> entries = new ArrayList<P2ResolutionResult.Entry>(result.getArtifacts());
        Collections.sort(entries, new Comparator<Entry>() {

            public int compare(Entry entry1, Entry entry2) {
                return entry1.getId().compareTo(entry2.getId());
            }
        });
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01", entries.get(0).getId());
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01.source", entries.get(1).getId());
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.feature01", entries.get(2).getId());
        Assert.assertEquals(bundle, entries.get(0).getLocation());
        Assert.assertEquals(bundle, entries.get(1).getLocation());
        Assert.assertEquals("sources", entries.get(1).getClassifier());
    }

    @Test
    public void testEclipseRepository() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());
        tpConfig.addP2Repository(resourceFile("repositories/launchers").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        projectToResolve = createReactorProject(resourceFile("resolver/repository"), TYPE_ECLIPSE_REPOSITORY,
                artifactId);

        addContextProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size()); // the product, bundle01, and the one dependency of bundle01
        Assert.assertEquals(3, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.osgi", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.tycho.p2.impl.resolver.test.bundle01", result.getNonReactorUnits());
    }

    @Test
    public void testBundleUsesSWT() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e361").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        projectToResolve = createReactorProject(resourceFile("resolver/bundleUsesSWT"), TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size());
        Assert.assertEquals(2, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.swt.gtk.linux.x86_64", result.getNonReactorUnits());
    }

    @Test
    public void testSwt() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        projectToResolve = createReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt");
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());
        assertContainLocation(result, swt);
    }

    @Test
    public void testSwtFragment() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        reactorProjects.add(createReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt"));
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        projectToResolve = createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN,
                "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainLocation(result, swt);
    }

    @Test
    public void testSwtFragmentWithRemoteSWT() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e361").toURI());

        File swtFragment = resourceFile("resolver/swt/swtFragment");
        projectToResolve = createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN,
                "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
    }

    private static void assertContainsUnit(String unitID, Set<?> units) {
        Assert.assertFalse("Unit " + unitID + " not found", getInstallableUnits(unitID, units).isEmpty());
    }

    private static List<IInstallableUnit> getInstallableUnits(String unitID, Set<?> units) {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        for (Object unitObject : units) {
            IInstallableUnit unit = (IInstallableUnit) unitObject;
            if (unitID.equals(unit.getId())) {
                result.add(unit);
            }
        }
        return result;
    }

    @Test
    public void testReactorVsExternal() throws Exception {
        tpConfig.addP2Repository(resourceFile("reactor-vs-external/extrepo").toURI());

        reactorProjects.add(createReactorProject(resourceFile("reactor-vs-external/bundle01"), TYPE_ECLIPSE_PLUGIN,
                "org.sonatype.tycho.p2.impl.resolver.test.bundle01"));

        projectToResolve = createReactorProject(resourceFile("reactor-vs-external/feature01"), TYPE_ECLIPSE_FEATURE,
                "org.sonatype.tycho.p2.impl.resolver.test.feature01");

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());

        for (Entry entry : result.getArtifacts()) {
            Assert.assertEquals("1.0.0.qualifier", entry.getVersion());
        }
    }

    @Test
    public void testResolutionRestrictedEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(
                getTargetPlatform(standardEEResolutionHintProvider("CDC-1.0/Foundation-1.0")), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());

        Assert.assertEquals(3, result.getNonReactorUnits().size());
        assertContainsUnit("javax.xml", result.getNonReactorUnits());
        assertContainsUnit("a.jre.cdc", result.getNonReactorUnits());
        assertContainsUnit("config.a.jre.cdc", result.getNonReactorUnits());
    }

    @Test
    public void testResolutionEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(
                getTargetPlatform(standardEEResolutionHintProvider("J2SE-1.5")), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());

        Assert.assertEquals(2, result.getNonReactorUnits().size());
        assertContainsUnit("a.jre.j2se", result.getNonReactorUnits());
        assertContainsUnit("config.a.jre.j2se", result.getNonReactorUnits());
    }

    @Test
    public void testResolutionCustomEE() throws Exception {
        // repository containing both a bundle and the custom profile providing javax.activation;version="1.1.1"
        tpConfig.addP2Repository(resourceFile("repositories/custom-profile").toURI());

        // bundle importing javax.activation;version="1.1.1"
        projectToResolve = createReactorProject(resourceFile("resolver/bundleRequiringVersionedJDKPackage"),
                TYPE_ECLIPSE_PLUGIN, "bundleRequiringVersionedJDKPackage");

        List<P2ResolutionResult> results = impl.resolveDependencies(
                getTargetPlatform(customEEResolutionHintProvider("Custom_Profile-2")), projectToResolve);

        assertThat(results.size(), is(1));
        P2ResolutionResult result = results.get(0); // huh?

        assertThat(result.getNonReactorUnits().size(), is(1));
        assertContainsUnit("a.jre.custom.profile", result.getNonReactorUnits());
        // I don't know why we should expect a config.a.jre.custom.profile IU here
    }

    @Test
    public void testFeatureWithUnresolvableSecondaryUnit() throws Exception {
        String artifactId = "feature.non-resolvable-p2-inf-unit";
        projectToResolve = createReactorProject(resourceFile("resolver/feature.non-resolvable-p2-inf-unit"),
                TYPE_ECLIPSE_FEATURE, artifactId);

        /*
         * The resolution only passes because the unresolvable, additional IU contributed via the
         * p2.inf is not a seed/primary unit. (Uncomment the "requires" lines in the p2.inf to see
         * this resolution fail.)
         */
        impl.resolveDependencies(getTargetPlatform(), projectToResolve);
    }

    @Test
    public void testFeatureMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        impl.setEnvironments(environments);

        String artifactId = "feature.multienv.p2-inf";
        projectToResolve = createReactorProject(resourceFile("resolver/feature.multienv.p2-inf"), TYPE_ECLIPSE_FEATURE,
                artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        Assert.assertEquals(1, linuxEntries.size());
        Assert.assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        Assert.assertEquals(1, macosxEntries.size());
        Assert.assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @Test
    public void testProductMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        impl.setEnvironments(environments);

        String artifactId = "product.multienv.p2-inf";
        projectToResolve = createReactorProject(resourceFile("resolver/product.multienv.p2-inf"),
                TYPE_ECLIPSE_REPOSITORY, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        Assert.assertEquals(1, linuxEntries.size());
        Assert.assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        Assert.assertEquals(1, macosxEntries.size());
        Assert.assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAdditionalFilterProperties() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle.filtered-dep";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.filtered-dep"), TYPE_ECLIPSE_PLUGIN,
                artifactId);

        impl.setAdditionalFilterProperties(Collections.singletonMap("org.example.custom.option", "true"));
        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
    }

    @Test
    public void testResolveWithoutProject() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        projectToResolve = null;
        impl.addDependency(TYPE_ECLIPSE_PLUGIN, "org.eclipse.osgi", "0.0.0");
        List<P2ResolutionResult> results = impl.resolveDependencies(getTargetPlatform(), projectToResolve);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
    }

    @Test
    public void testMissingArtifact() throws Exception {
        // repository with the IU org.eclipse.osgi but not the corresponding artifact (-> this repository is inconsistent, more often you'd get this situation in offline mode)
        tpConfig.addP2Repository(resourceFile("repositories/missing-artifact").toURI());
        // module requiring org.eclipse.osgi
        projectToResolve = createReactorProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN,
                "org.eclipse.tycho.p2.impl.resolver.test.bundle01");

        expectedException.expectMessage("could not be downloaded");
        impl.resolveDependencies(getTargetPlatform(), projectToResolve);
    }

    private P2TargetPlatform getTargetPlatform() {
        return tpFactory.createTargetPlatform(tpConfig, NOOP_EE_RESOLUTION_HANDLER, reactorProjects, pomDependencies);
    }

    private P2TargetPlatform getTargetPlatform(ExecutionEnvironmentResolutionHandler eeResolutionHandler) {
        return tpFactory.createTargetPlatform(tpConfig, eeResolutionHandler, reactorProjects, pomDependencies);
    }
}
