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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionResolutionException;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TargetDefinitionResolverTest {
    /** Feature including MAIN_BUNDLE and REFERENCED_BUNDLE_V1 */
    private static final IVersionedId TARGET_FEATURE = new VersionedId("trt.targetFeature.feature.group",
            "1.0.0.201108051343");

    /**
     * Bundle with unversioned dependency to REFERENCED_BUNDLE and optional dependency to
     * OPTIONAL_BUNDLE.
     */
    private static final IVersionedId MAIN_BUNDLE = new VersionedId("trt.bundle", "1.0.0.201108051343");
    private static final IVersionedId OPTIONAL_BUNDLE = new VersionedId("trt.bundle.optional", "1.0.0.201108051328");
    private static final IVersionedId REFERENCED_BUNDLE_V1 = new VersionedId("trt.bundle.referenced",
            "1.0.0.201108051343");
    private static final IVersionedId REFERENCED_BUNDLE_V2 = new VersionedId("trt.bundle.referenced",
            "2.0.0.201108051319");

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

        subject = new TargetDefinitionResolver(environments, p2Context.getAgent(), logger);
    }

    @Test
    public void testResolveOtherLocationYieldsWarning() throws Exception {
        TargetDefinition definition = definitionWith(new OtherLocationStub(), new LocationStub(TARGET_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
        assertThat(versionedIdsOf(content.getUnits()), hasItem(MAIN_BUNDLE));
        assertThat(logger.getWarnings(), hasItem("Target location type: Directory is not supported"));
    }

    @Test
    public void testResolveMultipleUnits() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE, REFERENCED_BUNDLE_V1));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleLocations() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(OPTIONAL_BUNDLE), new LocationStub(
                REFERENCED_BUNDLE_V1));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveMultipleRepositories() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, OPTIONAL_BUNDLE,
                REFERENCED_BUNDLE_V2));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(REFERENCED_BUNDLE_V2, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveNoRepositories() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.NONE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet()));
    }

    @Test
    public void testResolveIncludesDependencies() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, MAIN_BUNDLE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(MAIN_BUNDLE, REFERENCED_BUNDLE_V2, OPTIONAL_BUNDLE)));
    }

    @Test
    public void testResolveDependenciesAcrossLocations() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1, MAIN_BUNDLE),
                new LocationStub(TestRepositories.V2));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(MAIN_BUNDLE, REFERENCED_BUNDLE_V2, OPTIONAL_BUNDLE)));
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
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(REFERENCED_BUNDLE_V2)));
    }

    @Test
    public void testUnitWithExactVersion() {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, REFERENCED_BUNDLE_V1));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(REFERENCED_BUNDLE_V1)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnitWithWrongVersion() {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2,
                REFERENCED_BUNDLE_INVALID_VERSION));
        subject.resolveContent(definition);
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testInvalidRepository() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.INVALID, TARGET_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
    }

    @Test
    public void testResolveUsesPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.V1_AND_V2, TARGET_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units),
                is(versionedIdSet(TARGET_FEATURE, MAIN_BUNDLE, REFERENCED_BUNDLE_V1, OPTIONAL_BUNDLE)));
    }

    @Test(expected = TargetDefinitionResolutionException.class)
    public void testUnsatisfiedDependencyWithPlanner() throws Exception {
        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.UNSATISFIED, MAIN_BUNDLE));
        subject.resolveContent(definition);
    }

    static Set<IVersionedId> versionedIdsOf(Collection<? extends IInstallableUnit> units) {
        Set<IVersionedId> result = new HashSet<IVersionedId>();
        for (IInstallableUnit unit : units) {
            result.add(new VersionedId(unit.getId(), unit.getVersion()));
        }
        return result;
    }

    static Set<IVersionedId> versionedIdSet(IVersionedId... ids) {
        Set<IVersionedId> result = new HashSet<IVersionedId>();
        for (IVersionedId id : ids) {
            result.add(id);
        }
        return result;
    }

    static TargetDefinition definitionWith(Location... locations) {
        return new TargetDefinitionStub(Arrays.asList(locations));
    }

    static class TargetDefinitionStub implements TargetDefinition {
        private List<Location> locations;

        public TargetDefinitionStub(List<Location> locations) {
            this.locations = locations;
        }

        public List<Location> getLocations() {
            return locations;
        }
    }

    enum TestRepositories {
        NONE, V1, V2, V1_AND_V2, UNSATISFIED, INVALID, WITH_FILTERS
    }

    static class LocationStub implements TargetDefinition.InstallableUnitLocation {

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
            case WITH_FILTERS:
                return Collections.singletonList(new RepositoryStub("with-filters"));
            case INVALID:
                return Collections.singletonList(new RepositoryStub(null));
            case NONE:
                return Collections.emptyList();
            default:
                throw new RuntimeException();
            }
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
    }

    static class OtherLocationStub implements Location {
        public String getTypeDescription() {
            return "Directory";
        }
    }

    static class RepositoryStub implements TargetDefinition.Repository {

        private final String repository;

        public RepositoryStub(String repository) {
            this.repository = repository;
        }

        public URI getLocation() {
            try {
                if (repository != null) {
                    File repo = ResourceUtil.resourceFile("targetresolver/" + repository + "/content.xml")
                            .getParentFile();
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
