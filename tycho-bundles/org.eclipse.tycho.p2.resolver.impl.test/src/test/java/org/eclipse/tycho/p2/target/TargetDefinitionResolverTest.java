/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.eclipse.tycho.test.util.LogVerifier;
import org.eclipse.tycho.test.util.P2Context;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    public final P2Context p2Context = new P2Context();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();

    private TargetDefinitionResolver subject;

    @Before
    public void initContext() throws Exception {
        subject = new TargetDefinitionResolver(defaultEnvironments(),
                ExecutionEnvironmentTestUtils.NOOP_EE_RESOLUTION_HINTS, IncludeSourceMode.honor,
                new MockMavenContext(tempManager.newFolder("localRepo"), logVerifier.getLogger()), null);
    }

    static List<TargetEnvironment> defaultEnvironments() {
        return Collections.singletonList(new TargetEnvironment(null, null, null));
    }

    @Test
    public void testURI() throws URISyntaxException, MalformedURLException {
        String uri = TargetDefinitionResolver
                .convertRawToUri("file:C:\\ws\\target-testcase\\tycho\\target.references\\target.refs/base.target");
        //check that it has the missing / infront of the protocol
        assertTrue(uri + " does not start with file:/", uri.startsWith("file:/"));
        //check that this could be parsed as an URI afterwards
        URI parsed = new URI(uri);
        parsed.toURL();

    }

    @Test
    public void testResolveNoLocations() throws Exception {
        TargetDefinition definition = definitionWith();
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList()));
    }

    @Test
    public void testResolveOtherLocationYieldsWarning() throws Exception {
        TargetDefinition definition = definitionWith(new OtherLocationStub(), new LocationStub(TARGET_FEATURE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        logVerifier.expectWarning("Target location type 'OtherLocation' is not supported");
    }

    @Test
    public void testResolveMultipleUnits() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE, REFERENCED_BUNDLE_V1));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleLocations() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE),
                new LocationStub(REFERENCED_BUNDLE_V1));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleRepositories() throws Exception {
        TargetDefinition definition = definitionWith(
                new LocationStub(TestRepositories.V1_AND_V2, OPTIONAL_BUNDLE, REFERENCED_BUNDLE_V2));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V2, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveNoRepositories() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.NONE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList()));
    }

    @Test
    public void testResolveIncludesDependencies() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        assertThat(versionedIdsOf(units), hasItem(REFERENCED_BUNDLE_V1));
    }

    @Test
    public void testResolveDependenciesAcrossLocations() throws Exception {
        // TODO currently slicer treats every location as isolated so it warns about dependencies not being available here
        // but this behavior is confusing because the dependencies actually are available and planner would not warn
        // logVerifier.expectNoWarnings();
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.UNSATISFIED, TARGET_FEATURE),
                new LocationStub(TestRepositories.V1_AND_V2, MAIN_BUNDLE, REFERENCED_BUNDLE_V1));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), hasItem(MAIN_BUNDLE));
        assertThat(versionedIdsOf(units), hasItem(REFERENCED_BUNDLE_V1));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testMissingUnit() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V2, MAIN_BUNDLE));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnitOnlyLookedUpInLocation() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V2, MAIN_BUNDLE),
                new LocationStub(TestRepositories.V1));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test
    public void testUnitWithWildcardVersion() throws ProvisionException {
        TargetDefinition definition = definitionWith(
                new LocationStub(TestRepositories.V1_AND_V2, REFERENCED_BUNDLE_WILDCARD_VERSION));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V2)));
    }

    @Test
    public void testUnitWithExactVersion() throws ProvisionException {
        TargetDefinition definition = definitionWith(
                new LocationStub(TestRepositories.V1_AND_V2, REFERENCED_BUNDLE_V1));
        TargetDefinitionContent units = subject.resolveContent(definition, p2Context.getAgent());
        assertThat(versionedIdsOf(units), bagEquals(versionedIdList(REFERENCED_BUNDLE_V1)));
    }

    /**
     * Ideally, the interface should return strongly typed versions. Since this is not possible in
     * the facade, syntax errors in the version attribute can only be detected by the resolver.
     */
    @Test(expected = TargetDefinitionSyntaxException.class)
    public void testUnitWithWrongVersionYieldsSyntaxException() throws Exception {
        TargetDefinition definition = definitionWith(
                new LocationStub(TestRepositories.V1_AND_V2, REFERENCED_BUNDLE_INVALID_VERSION));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testInvalidRepository() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.INVALID, TARGET_FEATURE));
        subject.resolveContentWithExceptions(definition, p2Context.getAgent());
    }

    @Test
    public void testResolveWithBundleInclusionListYieldsWarning() throws ProvisionException {
        List<Location> noLocations = Collections.emptyList();
        TargetDefinition definition = new TargetDefinitionStub(noLocations, true);
        subject.resolveContent(definition, p2Context.getAgent());

        // this was bug 373776: the includeBundles tag (which is the selection on the Content tab) was silently ignored
        logVerifier.expectWarning("De-selecting bundles in a target definition file is not supported");

    }

    static <T> Matcher<Collection<T>> bagEquals(final Collection<T> collection) {
        return new TypeSafeMatcher<Collection<T>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("collection containing exactly " + collection);
            }

            @Override
            public boolean matchesSafely(Collection<T> item) {
                return item.size() == collection.size() && item.containsAll(collection) && collection.containsAll(item);
            }
        };
    }

    static Collection<IVersionedId> versionedIdsOf(TargetDefinitionContent content) {
        Collection<IVersionedId> result = new ArrayList<>();
        for (IInstallableUnit unit : content.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet()) {
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

        @Override
        public List<Location> getLocations() {
            return locations;
        }

        @Override
        public boolean hasIncludedBundles() {
            return hasBundleSelectionList;
        }

        @Override
        public String getOrigin() {
            return "test stub";
        }

        @Override
        public String getTargetEE() {
            return null;
        }

    }

    enum TestRepositories {
        NONE, V1, V2, V1_AND_V2, UNSATISFIED, INVALID, SOURCES
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

        @Override
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
            case NONE:
                return Collections.emptyList();
            case SOURCES:
                return Collections.singletonList(new RepositoryStub("include-source"));
            default:
                break;
            }
            throw new RuntimeException();
        }

        @Override
        public List<? extends Unit> getUnits() {
            List<UnitStub> result = new ArrayList<>();
            for (IVersionedId seedUnit : seedUnits) {
                result.add(new UnitStub(seedUnit));
            }
            return result;
        }

        @Override
        public String getTypeDescription() {
            return null;
        }

        @Override
        public IncludeMode getIncludeMode() {
            // the tests in this class work with either
            return IncludeMode.SLICER;
        }

        @Override
        public boolean includeAllEnvironments() {
            return false;
        }

        @Override
        public boolean includeSource() {
            return false;
        }
    }

    private static class OtherLocationStub implements Location {
        @Override
        public String getTypeDescription() {
            return "OtherLocation";
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

        @Override
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

        @Override
        public String getId() {
            return null;
        }

    }

    static class UnitStub implements Unit {

        private final IVersionedId unitReference;

        public UnitStub(IVersionedId targetFeature) {
            this.unitReference = targetFeature;
        }

        @Override
        public String getId() {
            return unitReference.getId();
        }

        @Override
        public String getVersion() {
            if (unitReference.getVersion() == INVALID_VERSION_MARKER) {
                return "abc";
            }
            return unitReference.getVersion().toString();
        }

    }

}
