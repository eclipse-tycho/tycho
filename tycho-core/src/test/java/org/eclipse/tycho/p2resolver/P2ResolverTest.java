/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - apply DRY principle
 *    Christoph Läubrich - adjust to changed API
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_REPOSITORY;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_TEST_PLUGIN;
import static org.eclipse.tycho.test.util.ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HANDLER;
import static org.eclipse.tycho.test.util.ExecutionEnvironmentTestUtils.customEEResolutionHintProvider;
import static org.eclipse.tycho.test.util.ExecutionEnvironmentTestUtils.standardEEResolutionHintProvider;
import static org.eclipse.tycho.test.util.InstallableUnitMatchers.unitWithId;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.ee.impl.ExecutionEnvironmentResolutionHandler;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentStub;
import org.eclipse.tycho.core.resolver.P2ResolutionResult;
import org.eclipse.tycho.core.resolver.P2ResolutionResult.Entry;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.target.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;
import org.eclipse.tycho.test.util.ArtifactMock;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.MockMavenContext;
import org.eclipse.tycho.test.util.ReactorProjectStub;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class P2ResolverTest extends P2ResolverTestBase {

    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private ReactorProject projectToResolve;
    private P2ResolutionResult result;
    private static final String LATEST_PLATFORM = "https://download.eclipse.org/eclipse/updates/latest/";
    protected P2Resolver impl;

    @Before
    public void initDefaultResolver() throws Exception {
//        org.eclipse.equinox.internal.p2.core.helpers.Tracing.DEBUG_PLANNER_PROJECTOR = true;
        pomDependencies = resolverFactory.newPomDependencyCollector();
        List<TargetEnvironment> environments = getEnvironments();
        impl = createFor(environments);
    }

    private P2ResolverImpl createFor(List<TargetEnvironment> environments) {
        return new P2ResolverImpl(tpFactory, null, logVerifier.getMavenLogger(), environments);
    }

    @Test
    public void testBasic() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342_2").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(2, result.getNonReactorUnits().size()); // + a.jre
    }

    @Test
    public void testDuplicateInstallableUnit() throws Exception {
        projectToResolve = createReactorProject(resourceFile("duplicate-iu/featureA"), TYPE_ECLIPSE_FEATURE,
                "featureA");
        reactorProjects
                .add(createReactorProject(resourceFile("duplicate-iu/featureA2"), TYPE_ECLIPSE_FEATURE, "featureA2"));

        // TODO 353889 make the duplicate detection work without having the current project IUs in the target platform
        reactorProjects.add(projectToResolve);

        assertThrows(DuplicateReactorIUsException.class,
                () -> impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));
    }

    @Test
    public void testProjectToResolveOverwritesTargetContent() throws Exception {
        reactorProjects.add(createReactorProject(resourceFile("resolver/bundle.optional-dep"), TYPE_ECLIPSE_PLUGIN,
                "bundle.optional-dep", OptionalResolutionAction.IGNORE));
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.optional-dep"), TYPE_ECLIPSE_PLUGIN,
                "bundle.optional-dep", OptionalResolutionAction.REQUIRE);

        tpConfig.addP2Repository(resourceFile("repositories/e342_2").toURI());
        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertThat(result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
        // the unit from projectToResolve with the dependency has been used
    }

    @Test
    public void testFeatureInstallableUnits() throws Exception {
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        projectToResolve = createReactorProject(resourceFile("resolver/feature01"), TYPE_ECLIPSE_FEATURE, artifactId);

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(1, result.getArtifacts().size());
        assertEquals(1, result.getArtifacts().iterator().next().getInstallableUnits().size());
        assertEquals(0, result.getNonReactorUnits().size());
    }

    @Test
    public void testSourceBundle() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/jre").toURI());
        String featureId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        projectToResolve = createReactorProject(resourceFile("sourcebundles/feature01"), TYPE_ECLIPSE_FEATURE,
                featureId);

        File bundle = resourceFile("sourcebundles/bundle01");
        String bundleId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String bundleVersion = "1.0.0-SNAPSHOT";
        reactorProjects.add(createReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, bundleId));

        ReactorProjectStub sb = new ReactorProjectStub(bundle, bundleId, bundleId, bundleVersion, TYPE_ECLIPSE_PLUGIN);
        SourcesBundleDependencyMetadataGenerator metadata = new SourcesBundleDependencyMetadataGenerator(new MockMavenContext(null, logVerifier.getLogger()), null);
        DependencyMetadata generateMetadata = metadata.generateMetadata(new ArtifactMock(sb, "source"),
                getEnvironments(), null, new PublisherOptions());
        sb.setDependencyMetadata(generateMetadata);
        reactorProjects.add(sb);

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(false), projectToResolve));

        assertEquals(3, result.getArtifacts().size());
        List<P2ResolutionResult.Entry> entries = new ArrayList<>(result.getArtifacts());
        Collections.sort(entries, (entry1, entry2) -> entry1.getId().compareTo(entry2.getId()));
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01", entries.get(0).getId());
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01.source", entries.get(1).getId());
        assertEquals("org.eclipse.tycho.p2.impl.resolver.test.feature01", entries.get(2).getId());
        assertEquals(bundle, entries.get(0).getLocation(true));
        assertEquals(bundle, entries.get(1).getLocation(true));
        assertEquals("sources", entries.get(1).getClassifier());
    }

    @Test
    public void testEclipseRepository() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342_2").toURI());
        tpConfig.addP2Repository(resourceFile("repositories/launchers").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        projectToResolve = createReactorProject(resourceFile("resolver/repository"), TYPE_ECLIPSE_REPOSITORY,
                artifactId);

        addContextProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN);

        P2TargetPlatform targetPlatform = getTargetPlatform();
        Map<TargetEnvironment, P2ResolutionResult> map = impl.resolveTargetDependencies(targetPlatform,
                projectToResolve);
        result = singleEnv(map);

        assertEquals(3, result.getArtifacts().size()); // the product, bundle01, and the one dependency of bundle01
        assertEquals(4, result.getNonReactorUnits().size()); // + a.jre

        assertContainsUnit("org.eclipse.osgi", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.tycho.p2.impl.resolver.test.bundle01", result.getNonReactorUnits());
    }

    @Test
    public void testEclipseTestPluginAutomaticallyDependsOnTestHarnesses() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342").toURI());
        addContextProject(resourceFile("resolver/bundle.uitestharness"), TYPE_ECLIPSE_PLUGIN);

        projectToResolve = createReactorProject(resourceFile("resolver/bundle.nodeps"), TYPE_ECLIPSE_TEST_PLUGIN,
                "bundle.nodeps");

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertThat(result.getArtifacts(), hasItem(withId("bundle.nodeps")));
        assertThat(result.getArtifacts(), hasItem(withId("org.eclipse.ui.ide.application")));
    }

    @Test
    public void testBundleUsesSWT() throws Exception {
        tpConfig.addP2Repository(new URI(LATEST_PLATFORM));

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        projectToResolve = createReactorProject(resourceFile("resolver/bundleUsesSWT"), TYPE_ECLIPSE_PLUGIN,
                artifactId);

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(4, result.getArtifacts().size());
        assertEquals(4, result.getNonReactorUnits().size()); // + a.jre

        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.swt.gtk.linux.x86_64", result.getNonReactorUnits());
    }

    @Test
    public void testSwt() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        projectToResolve = createReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt");
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        createReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

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

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(false), projectToResolve));

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

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

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

        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(false), projectToResolve));

        assertEquals(2, result.getArtifacts().size());
        assertEquals(1, result.getNonReactorUnits().size()); // a.jre

        for (Entry entry : result.getArtifacts()) {
            assertEquals("1.0.0.qualifier", entry.getVersion());
        }
    }

    @Test
    public void testResolutionRestrictedEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveTargetDependencies(
                getTargetPlatform(
                        standardEEResolutionHintProvider(new ExecutionEnvironmentStub("CDC-1.0/Foundation-1.0"))),
                projectToResolve));

        assertEquals(2, result.getArtifacts().size());

        assertEquals(2, result.getNonReactorUnits().size());
        assertContainsUnit("javax.xml", result.getNonReactorUnits());
        assertContainsUnit("a.jre.cdc", result.getNonReactorUnits());
    }

    @Test
    public void testResolutionEE() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        String artifactId = "bundle.bree";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.bree"), TYPE_ECLIPSE_PLUGIN, artifactId);

        result = singleEnv(impl.resolveTargetDependencies(
                getTargetPlatform(
                        standardEEResolutionHintProvider(new ExecutionEnvironmentStub("J2SE-1.5", "org.w3c.dom"))),
                projectToResolve));

        assertEquals(1, result.getArtifacts().size());

        assertEquals(1, result.getNonReactorUnits().size());
        assertContainsUnit("a.jre.j2se", result.getNonReactorUnits());
    }

    @Test
    public void testResolutionCustomEE() throws Exception {
        // repository containing both a bundle and the custom profile providing javax.activation;version="1.1.1"
        tpConfig.addP2Repository(resourceFile("repositories/custom-profile").toURI());

        // bundle importing javax.activation;version="1.1.1"
        projectToResolve = createReactorProject(resourceFile("resolver/bundleRequiringVersionedJDKPackage"),
                TYPE_ECLIPSE_PLUGIN, "bundleRequiringVersionedJDKPackage");

        result = singleEnv(impl.resolveTargetDependencies(
                getTargetPlatform(customEEResolutionHintProvider("Custom_Profile-2")), projectToResolve));

        assertEquals(1, result.getNonReactorUnits().size());
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
        impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve);
    }

    @Test
    public void testP2InfUnclassifiedBundleUnitDoesntOverwriteMainArtifact() throws Exception {
        // project with feature.xml and p2.inf contributing a bundle IU -> type of main artifact cannot be determined
        projectToResolve = createReactorProject(resourceFile("resolver/p2Inf.conflicting-main-artifact"),
                TYPE_ECLIPSE_FEATURE, "p2Inf.conflicting-main-artifact");

        // bug 430728: explicit detect this conflict and throw an exception
        Exception e = assertThrows(Exception.class, () -> {
            result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));
            // ... or the p2.inf "artifact" could also just be omitted
            assertEquals(ArtifactType.TYPE_ECLIPSE_FEATURE, getClassifiedArtifact(result, null).getType());
            assertEquals(1, result.getArtifacts().size());
        });
        assertTrue(e.getMessage().contains("classifier"));

    }

    @Test
    public void testP2InfClassifiedBundleUnitDoesntOverwriteMainArtifact() throws Exception {
        // bug 430728: correct way to add the bundle IU via p2.inf in the the feature project
        projectToResolve = createReactorProject(resourceFile("resolver/p2Inf.additional-artifact"),
                TYPE_ECLIPSE_FEATURE, "p2Inf.additional-artifact");
        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertEquals(ArtifactType.TYPE_ECLIPSE_FEATURE, getClassifiedArtifact(result, null).getType());
    }

    @Test
    public void testFeatureMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        P2Resolver resolver = createFor(environments);

        String artifactId = "feature.multienv.p2-inf";
        projectToResolve = createReactorProject(resourceFile("resolver/feature.multienv.p2-inf"), TYPE_ECLIPSE_FEATURE,
                artifactId);

        List<P2ResolutionResult> results = new ArrayList<>(
                resolver.resolveTargetDependencies(getTargetPlatform(), projectToResolve).values());

        assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<>(linux.getArtifacts());
        assertEquals(1, linuxEntries.size());
        assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<>(macosx.getArtifacts());
        assertEquals(1, macosxEntries.size());
        assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @Test
    public void testProductMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        P2Resolver resolver = createFor(environments);

        String artifactId = "product.multienv.p2-inf";
        projectToResolve = createReactorProject(resourceFile("resolver/product.multienv.p2-inf"),
                TYPE_ECLIPSE_REPOSITORY, artifactId);

        List<P2ResolutionResult> results = new ArrayList<>(
                resolver.resolveTargetDependencies(getTargetPlatform(), projectToResolve).values());

        assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<>(linux.getArtifacts());
        assertEquals(1, linuxEntries.size());
        assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<>(macosx.getArtifacts());
        assertEquals(1, macosxEntries.size());
        assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @Test
    public void testAdditionalFilterProperties() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342_2").toURI());

        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle.filtered-dep";
        projectToResolve = createReactorProject(resourceFile("resolver/bundle.filtered-dep"), TYPE_ECLIPSE_PLUGIN,
                artifactId);

        impl.setAdditionalFilterProperties(Collections.singletonMap("org.example.custom.option", "true"));
        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertThat(result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
    }

    @Test
    public void testResolveWithoutProject() throws Exception {
        tpConfig.addP2Repository(resourceFile("repositories/e342_2").toURI());

        projectToResolve = null;
        impl.addDependency(TYPE_ECLIPSE_PLUGIN, "org.eclipse.osgi", "0.0.0");
        result = singleEnv(impl.resolveTargetDependencies(getTargetPlatform(), projectToResolve));

        assertThat(result.getNonReactorUnits(), hasItem(unitWithId("org.eclipse.osgi")));
    }

    private P2TargetPlatform getTargetPlatform() {
        return getTargetPlatform(NOOP_EE_RESOLUTION_HANDLER);
    }

    private P2TargetPlatform getTargetPlatform(boolean finalTarget) {
        return getTargetPlatform(NOOP_EE_RESOLUTION_HANDLER, finalTarget);
    }

    private P2TargetPlatform getTargetPlatform(ExecutionEnvironmentResolutionHandler eeResolutionHandler) {
        return getTargetPlatform(eeResolutionHandler, true);
    }

    private P2TargetPlatform getTargetPlatform(ExecutionEnvironmentResolutionHandler eeResolutionHandler,
            boolean finalTarget) {
        P2TargetPlatform preliminaryTP = tpFactory.createTargetPlatform(tpConfig, eeResolutionHandler, reactorProjects);
        if (finalTarget) {
            return tpFactory.createTargetPlatformWithUpdatedReactorContent(preliminaryTP, Collections.emptyList(),
                    pomDependencies);
        } else {
            return preliminaryTP;
        }
    }

    private static P2ResolutionResult singleEnv(Map<TargetEnvironment, P2ResolutionResult> map) {
        assertEquals(1, map.size());
        return map.values().iterator().next();
    }

    private static P2ResolutionResult.Entry getClassifiedArtifact(P2ResolutionResult resolutionResult,
            String classifier) {
        Set<String> availableClassifiers = new HashSet<>();
        P2ResolutionResult.Entry selectedEntry = null;
        for (Entry entry : resolutionResult.getArtifacts()) {
            availableClassifiers.add(entry.getClassifier());
            if (Objects.equals(classifier, entry.getClassifier())) {
                selectedEntry = entry;
            }
        }
        assertThat(availableClassifiers, hasItem(classifier));
        return selectedEntry;
    }

    static Matcher<Entry> withId(final String id) {
        return new TypeSafeMatcher<>() {

            @Override
            protected boolean matchesSafely(Entry entry) {
                return id.equals(entry.getId());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("an artifact with ID " + id);
            }
        };
    }

    private static void assertContainsUnit(String unitID, Set<?> units) {
        assertFalse("Unit " + unitID + " not found", getInstallableUnits(unitID, units).isEmpty());
    }

    private static List<IInstallableUnit> getInstallableUnits(String unitID, Set<?> units) {
        List<IInstallableUnit> result = new ArrayList<>();
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
            if (entry.getLocation(true).equals(location)) {
                return;
            }
        }
        fail();
    }

}
