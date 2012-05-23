/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverIncludeModeTests.PlannerLocationStub;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.test.util.P2Context;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;

public class TargetDefinitionResolverTest {
    /** Feature including MAIN_BUNDLE and REFERENCED_BUNDLE_V1 */
    static final IVersionedId TARGET_FEATURE = new VersionedId("trt.targetFeature.feature.group", "1.0.0.201108051343");

    /**
     * Bundle with unversioned dependency to REFERENCED_BUNDLE and optional dependency to
     * OPTIONAL_BUNDLE.
     */
    static final IVersionedId MAIN_BUNDLE = new VersionedId("trt.bundle", "1.0.0.201108051343");
    static final IVersionedId OPTIONAL_BUNDLE = new VersionedId("trt.bundle.optional", "1.0.0.201108051328");
    static final IVersionedId REFERENCED_BUNDLE_V1 = new VersionedId("trt.bundle.referenced", "1.0.0.201108051343");
    static final IVersionedId REFERENCED_BUNDLE_V2 = new VersionedId("trt.bundle.referenced", "2.0.0.201108051319");

    private static final IVersionedId REFERENCED_BUNDLE_WILDCARD_VERSION = new VersionedId("trt.bundle.referenced",
            "0.0.0");

    private static final Version INVALID_VERSION_MARKER = Version.parseVersion("1.1.1.broken-marker");
    private static final VersionedId REFERENCED_BUNDLE_INVALID_VERSION = new VersionedId("trt.bundle.referenced",
            INVALID_VERSION_MARKER);

    @Rule
    public P2Context p2Context = new P2Context();

    private MavenLoggerStub logger = new MavenLoggerStub();
    private TargetDefinitionResolver subject;

    @Before
    public void initContext() throws Exception {
        Map<String, String> emptyMap = new HashMap<String, String>();
        List<Map<String, String>> environments = Collections.singletonList(emptyMap);

        subject = new TargetDefinitionResolver(environments, new JREInstallableUnits(null), p2Context.getAgent(),
                logger);
    }

    @Test
    public void testResolveNoLocations() throws Exception {
        TargetDefinition definition = definitionWith();
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList()));
    }

    @Test
    public void testResolveOtherLocationYieldsWarning() throws Exception {
        TargetDefinition definition = definitionWith(new OtherLocationStub(), new LocationStub(TARGET_FEATURE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        assertThat(logger.getWarnings(), hasItem("Target location type: Directory is not supported"));
    }

    @Test
    public void testResolveMultipleUnits() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE, REFERENCED_BUNDLE_V1));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleLocations() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE), new LocationStub(
                REFERENCED_BUNDLE_V1));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleRepositories() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, OPTIONAL_BUNDLE,
                REFERENCED_BUNDLE_V2));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V2, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveNoRepositories() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.NONE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList()));
    }

    @Test
    public void testResolveIncludesDependencies() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        assertThat(versionedIdsOf(units), hasItem(REFERENCED_BUNDLE_V1));
    }

    @Test
    public void testResolveDependenciesAcrossLocations() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE),
                new LocationStub(TestRepositories.V1_AND_V2));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        assertThat(versionedIdsOf(units), hasItem(REFERENCED_BUNDLE_V1));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testMissingUnit() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V2, MAIN_BUNDLE));
        subject.resolveContent(definition);
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnitOnlyLookedUpInLocation() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V2, MAIN_BUNDLE),
                new LocationStub(TestRepositories.V1));
        subject.resolveContent(definition);
    }

    @Test
    public void testUnitWithWildcardVersion() {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2,
                REFERENCED_BUNDLE_WILDCARD_VERSION));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V2)));
    }

    @Test
    public void testUnitWithExactVersion() {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, REFERENCED_BUNDLE_V1));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1)));
    }

    /**
     * Ideally, the interface should return strongly typed versions. Since this is not possible in
     * the facade, syntax errors in the version attribute can only be detected by the resolver.
     */
    @Test(expected = TargetDefinitionSyntaxException.class)
    public void testUnitWithWrongVersionYieldsSyntaxException() {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2,
                REFERENCED_BUNDLE_INVALID_VERSION));
        subject.resolveContent(definition);
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testInvalidRepository() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.INVALID, TARGET_FEATURE));
        subject.resolveContent(definition);
    }

    @Test
    public void testRestrictedExecutionEnvironment() throws Exception {
        Map<String, String> emptyMap = new HashMap<String, String>();
        List<Map<String, String>> environments = Collections.singletonList(emptyMap);

        subject = new TargetDefinitionResolver(environments, new JREInstallableUnits("CDC-1.0/Foundation-1.0"),
                p2Context.getAgent(), logger);

        VersionedId seed = new VersionedId("dom-client", "0.0.1.SNAPSHOT");
        TargetDefinition definition = definitionWith(new PlannerLocationStub(TestRepositories.JAVAXXML, seed));
        TargetPlatformContent units = subject.resolveContent(definition);
        assertThat(versionedIdsOf(units),
                bagEquals(versionedIdList(seed, new VersionedId("javax.xml", "0.0.1.SNAPSHOT"))));

    }

    @Test
    public void test370502_requireJREIUs() throws Exception {
        Map<String, String> emptyMap = new HashMap<String, String>();
        List<Map<String, String>> environments = Collections.singletonList(emptyMap);

        subject = new TargetDefinitionResolver(environments, new JREInstallableUnits("J2SE-1.5"), p2Context.getAgent(),
                logger);

        VersionedId seed = new VersionedId("sdk", "1.0.0");
        TargetPlatformContent units = subject.resolveContent(definitionWith(new PlannerLocationStub(
                TestRepositories.REQUIREJREIUS, seed)));
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(seed)));

        units = subject.resolveContent(definitionWith(new LocationStub(TestRepositories.REQUIREJREIUS, seed)));
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(seed)));
    }

    @Test
    public void testResolveWithBundleInclusionListYieldsWarning() {
        List<Location> noLocations = Collections.emptyList();
        TargetDefinition definition = new TargetDefinitionStub(noLocations, true);
        subject.resolveContent(definition);

        // this was bug 373776: the includeBundles tag (which is the selection on the Content tab) was silently ignored
        assertThat(logger.getWarnings(),
                hasItem(containsString("De-selecting bundles in a target definition file is not supported")));

    }

    static <T> Matcher<Collection<T>> bagEquals(final Collection<T> collection) {
        return new TypeSafeMatcher<Collection<T>>() {

            public void describeTo(Description description) {
                description.appendText("collection containing exactly " + collection);
            }

            @Override
            public boolean matchesSafely(Collection<T> item) {
                return item.size() == collection.size() && item.containsAll(collection) && collection.containsAll(item);
            }
        };
    }

    static Collection<IVersionedId> versionedIdsOf(TargetPlatformContent content) {
        Collection<IVersionedId> result = new ArrayList<IVersionedId>();
        for (IInstallableUnit unit : content.getUnits()) {
            result.add(new VersionedId(unit.getId(), unit.getVersion()));
        }
        return result;
    }

    static Collection<IVersionedId> versionedIdList(IVersionedId... ids) {
        return Arrays.asList(ids);
    }

    static TargetDefinition definitionWith(Location... locations) {
        return new TargetDefinitionStub(Arrays.asList(locations));
    }

    static class TargetDefinitionStub implements TargetDefinition {
        private List<Location> locations;
        private final boolean hasBundleSelectionList;

        public TargetDefinitionStub(List<Location> locations) {
            this(locations, false);
        }

        public TargetDefinitionStub(List<Location> locations, boolean hasBundleSelectionList) {
            this.locations = locations;
            this.hasBundleSelectionList = hasBundleSelectionList;
        }

        public List<Location> getLocations() {
            return locations;
        }

        public boolean hasIncludedBundles() {
            return hasBundleSelectionList;
        }
    }

    enum TestRepositories {
        NONE, V1, V2, V1_AND_V2, UNSATISFIED, INVALID, JAVAXXML, REQUIREJREIUS
    }

    static class LocationStub implements InstallableUnitLocation {

        private final IVersionedId[] seedUnits;
        private final TestRepositories repositories;

        LocationStub(TestRepositories repositories, IVersionedId... seedUnits) {
            this.repositories = repositories;
            this.seedUnits = seedUnits;
        }

        LocationStub(IVersionedId... seedUnits) {
            this(TestRepositories.V1, seedUnits);
        }

        public List<? extends Repository> getRepositories() {
            switch (repositories) {
            case V1:
                return Collections.singletonList(new RepositoryStub("v1_content"));
            case V2:
                return Collections.singletonList(new RepositoryStub("v2_content"));
            case V1_AND_V2:
                return Arrays.asList(new RepositoryStub("v1_content"), new RepositoryStub("v2_content"));
            case UNSATISFIED:
                return Collections.singletonList(new RepositoryStub("unsatisfied"));
            case INVALID:
                return Collections.singletonList(new RepositoryStub(null));
            case JAVAXXML:
                return Collections.singletonList(new RepositoryStub("repositories/", "javax.xml"));
            case REQUIREJREIUS:
                return Collections.singletonList(new RepositoryStub("repositories/", "requirejreius"));
            case NONE:
                return Collections.emptyList();
            }
            throw new RuntimeException();
        }

        public List<? extends Unit> getUnits() {
            List<UnitStub> result = new ArrayList<UnitStub>();
            for (IVersionedId seedUnit : seedUnits) {
                result.add(new UnitStub(seedUnit));
            }
            return result;
        }

        public String getTypeDescription() {
            return null;
        }

        public IncludeMode getIncludeMode() {
            // the tests in this class work with either
            return IncludeMode.SLICER;
        }

        public boolean includeAllEnvironments() {
            return false;
        }
    }

    private static class OtherLocationStub implements Location {
        public String getTypeDescription() {
            return "Directory";
        }
    }

    static class RepositoryStub implements Repository {

        private final String basedir;
        private final String repository;

        public RepositoryStub(String basedir, String repository) {
            this.basedir = basedir;
            this.repository = repository;
        }

        public RepositoryStub(String repository) {
            this("targetresolver/", repository);
        }

        public URI getLocation() {
            try {
                if (repository != null) {
                    File repo = ResourceUtil.resourceFile(basedir + repository + "/content.xml").getParentFile();
                    return repo.toURI();
                }
                return URI.create("invalid:hello");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getId() {
            return null;
        }

    }

    static class UnitStub implements Unit {

        private final IVersionedId unitReference;

        public UnitStub(IVersionedId targetFeature) {
            this.unitReference = targetFeature;
        }

        public String getId() {
            return unitReference.getId();
        }

        public String getVersion() {
            if (unitReference.getVersion() == INVALID_VERSION_MARKER) {
                return "abc";
            }
            return unitReference.getVersion().toString();
        }

    }
}
