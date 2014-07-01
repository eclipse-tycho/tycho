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

import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_REPOSITORY;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_UPDATE_SITE;
import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.customEEResolutionHintProvider;
import static org.eclipse.tycho.p2.target.ExecutionEnvironmentTestUtils.standardEEResolutionHintProvider;
import static org.eclipse.tycho.p2.testutil.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactType;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class P2ResolverTest extends P2ResolverTestBase {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private ReactorProject projectToResolve;
    private P2ResolutionResult result;

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

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(1, result.getNonReactorUnits().size());
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

        result = impl.collectProjectDependencies(getTargetPlatform(), projectToResolve);

        assertEquals(projects.length, result.getArtifacts().size());
        for (File project : projects) {
            assertContainLocation(result, project);
        }
        // TODO the eclipse-update-site module itself is not in the resolution result; is this needed?

        // conflicting dependency mode only collects included artifacts - the referenced non-reactor unit
        // org.eclipse.osgi is not included
        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), not(hasItem(unitWithId("org.eclipse.osgi"))));
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
        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
        // the unit from projectToResolve with the dependency has been used
    }

    @Test
    public void testFeatureInstallableUnits() throws Exception {
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        projectToResolve = createReactorProject(resourceFile("resolver/feature01"), TYPE_ECLIPSE_FEATURE, artifactId);

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(1, result.getArtifacts().size());
        assertEquals(1, result.getArtifacts().iterator().next().getInstallableUnits().size());
        assertEquals(0, result.getNonReactorUnits().size());
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

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(3, result.getArtifacts().size());
        List<P2ResolutionResult.Entry> entries = new ArrayList<P2ResolutionResult.Entry>(result.getArtifacts());
        Collections.sort(entries, new Comparator<Entry>() {

            public int compare(Entry entry1, Entry entry2) {
                return entry1.getId().compareTo(entry2.getId());
            }
        });
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01", entries.get(0).getId());
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01.source", entries.get(1).getId());
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.feature01", entries.get(2).getId());
        assertEquals(bundle, entries.get(0).getLocation());
        assertEquals(bundle, entries.get(1).getLocation());
        assertEquals("sources", entries.get(1).getClassifier());
    }

    @Test
    public void testEclipseRepository() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());
        tpConfig.addP2Repository(resourceFile("repositories/launchers").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        projectToResolve = createReactorProject(resourceFile("resolver/repository"), TYPE_ECLIPSE_REPOSITORY,
                artifactId);

        addContextProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN);

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(3, result.getArtifacts().size()); // the product, bundle01, and the one dependency of bundle01
        assertEquals(3, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.osgi", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.tycho.p2.impl.resolver.test.bundle01", result.getNonReactorUnits());
    }

    @Test
    public void testBundleUsesSWT() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e361").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        projectToResolve = createReactorProject(resourceFile("resolver/bundleUsesSWT"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(3, result.getArtifacts().size());
        assertEquals(2, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.swt.gtk.linux.x86_64", result.getNonReactorUnits());
    }

    @Test
    public void testSwt() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        projectToResolve = createReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt");
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(1, result.getArtifacts().size());
        assertContainLocation(result, swt);
    }

    @Test
    public void testSwtFragment() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        reactorProjects.add(createReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt"));
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        projectToResolve = createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN,
                "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(0, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainLocation(result, swt);
    }

    @Test
    public void testSwtFragmentWithRemoteSWT() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e361").toURI());

        File swtFragment = resourceFile("resolver/swt/swtFragment");
        projectToResolve = createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN,
                "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(1, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
    }

    @Test
    public void testReactorVsExternal() throws Exception {
        tpConfig.addP2Repository(resourceFile("reactor-vs-external/extrepo").toURI());

        reactorProjects.add(createReactorProject(resourceFile("reactor-vs-external/bundle01"), TYPE_ECLIPSE_PLUGIN,
                "org.sonatype.tycho.p2.impl.resolver.test.bundle01"));

        projectToResolve = createReactorProject(resourceFile("reactor-vs-external/feature01"), TYPE_ECLIPSE_FEATURE,
                "org.sonatype.tycho.p2.impl.resolver.test.feature01");

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(0, result.getNonReactorUnits().size());

        for (Entry entry : result.getArtifacts()) {
            assertEquals("1.0.0.qualifier", entry.getVersion());
        }
    }

    @Test
    public void testResolutionRestrictedEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveDependencies(
                getTargetPlatform(standardEEResolutionHintProvider("CDC-1.0/Foundation-1.0")), projectToResolve));

        assertEquals(2, result.getArtifacts().size());

        assertEquals(3, result.getNonReactorUnits().size());
        assertContainsUnit("javax.xml", result.getNonReactorUnits());
        assertContainsUnit("a.jre.cdc", result.getNonReactorUnits());
        assertContainsUnit("config.a.jre.cdc", result.getNonReactorUnits());
    }

    @Test
    public void testResolutionEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveDependencies(getTargetPlatform(standardEEResolutionHintProvider("J2SE-1.5")),
                projectToResolve));

        assertEquals(1, result.getArtifacts().size());

        assertEquals(2, result.getNonReactorUnits().size());
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

        result = singleEnv(impl.resolveDependencies(
                getTargetPlatform(customEEResolutionHintProvider("Custom_Profile-2")), projectToResolve));

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
    public void testP2InfUnclassifiedBundleUnitDoesntOverwriteMainArtifact() throws Exception {
        // project with feature.xml and p2.inf contributing a bundle IU -> type of main artifact cannot be determined
        projectToResolve = createReactorProject(resourceFile("resolver/p2Inf.conflicting-main-artifact"),
                TYPE_ECLIPSE_FEATURE, "p2Inf.conflicting-main-artifact");

        // bug 430728: explicit detect this conflict and throw an exception 
        expectedException.expectMessage("classifier");
        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        // ... or the p2.inf "artifact" could also just be omitted
        assertThat(result.getArtifacts().size(), is(1));
        assertThat(getClassifiedArtifact(result, null).getType(), is(ArtifactType.TYPE_ECLIPSE_FEATURE));
    }

    @Test
    public void testP2InfClassifiedBundleUnitDoesntOverwriteMainArtifact() throws Exception {
        // bug 430728: correct way to add the bundle IU via p2.inf in the the feature project
        projectToResolve = createReactorProject(resourceFile("resolver/p2Inf.additional-artifact"),
                TYPE_ECLIPSE_FEATURE, "p2Inf.additional-artifact");
        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertThat(getClassifiedArtifact(result, null).getType(), is(ArtifactType.TYPE_ECLIPSE_FEATURE));
        // this is the current behaviour, but the p2.inf "artifact" could also be omitted
        //assertThat(getClassifiedArtifact(result, "p2inf").getType(), is(ArtifactType.TYPE_ECLIPSE_PLUGIN));
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

        assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        assertEquals(1, linuxEntries.size());
        assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        assertEquals(1, macosxEntries.size());
        assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, macosx.getNonReactorUnits().size());
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

        assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        assertEquals(1, linuxEntries.size());
        assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        assertEquals(1, macosxEntries.size());
        assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAdditionalFilterProperties() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle.filtered-dep";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.filtered-dep"), TYPE_ECLIPSE_PLUGIN,
                artifactId);

        impl.setAdditionalFilterProperties(Collections.singletonMap("org.example.custom.option", "true"));
        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
    }

    @Test
    public void testResolveWithoutProject() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());

        projectToResolve = null;
        impl.addDependency(TYPE_ECLIPSE_PLUGIN, "org.eclipse.osgi", "0.0.0");
        result = singleEnv(impl.resolveDependencies(getTargetPlatform(), projectToResolve));

        assertThat((Set<IInstallableUnit>) result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
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

    private static P2ResolutionResult singleEnv(List<P2ResolutionResult> results) {
        assertEquals(1, results.size());
        return results.get(0);
    }

    private static P2ResolutionResult.Entry getClassifiedArtifact(P2ResolutionResult resolutionResult, String classifier) {
        Set<String> availableClassifiers = new HashSet<String>();
        P2ResolutionResult.Entry selectedEntry = null;
        for (Entry entry : resolutionResult.getArtifacts()) {
            availableClassifiers.add(entry.getClassifier());
            if (eq(classifier, entry.getClassifier())) {
                selectedEntry = entry;
            }
        }
        assertThat(availableClassifiers, hasItem(classifier));
        return selectedEntry;
    }

    private static void assertContainsUnit(String unitID, Set<?> units) {
        assertFalse("Unit " + unitID + " not found", getInstallableUnits(unitID, units).isEmpty());
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

    private static void assertContainLocation(P2ResolutionResult result, File location) {
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            if (entry.getLocation().equals(location)) {
                return;
            }
        }
        fail();
    }

    private static boolean eq(String left, String right) {
        if (left == right) {
            return true;
        } else if (left == null) {
            return false;
        } else {
            return left.equals(right);
        }
    }

}
