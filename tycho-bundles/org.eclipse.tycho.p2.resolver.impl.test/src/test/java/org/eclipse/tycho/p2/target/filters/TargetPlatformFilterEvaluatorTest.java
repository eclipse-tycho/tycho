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
package org.eclipse.tycho.p2.target.filters;

import static org.eclipse.tycho.artifacts.TargetPlatformFilter.removeAllFilter;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.restrictionFilter;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithVersion;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithVersionRange;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithoutVersion;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.TargetPlatformFilterSyntaxException;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;

public class TargetPlatformFilterEvaluatorTest {

    private static final CapabilityPattern ALL_MULTIVERSION_BUNDLES = patternWithVersion(CapabilityType.OSGI_BUNDLE,
            "trf.bundle.multiversion", null);

    @Rule
    public P2Context p2Context = new P2Context();

    private Set<IInstallableUnit> baselineUnits;
    private LinkedHashSet<IInstallableUnit> workUnits;

    private TargetPlatformFilterEvaluator subject;

    @Before
    public void setUp() throws Exception {
        // TODO do this in beforeClass -> requires @ClassRule from junit >= 4.9
        baselineUnits = loadTestUnits();

        workUnits = new LinkedHashSet<IInstallableUnit>(baselineUnits);
    }

    private Set<IInstallableUnit> loadTestUnits() throws Exception {
        IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) p2Context.getAgent().getService(
                IMetadataRepositoryManager.SERVICE_NAME);
        File testDataFile = ResourceUtil.resourceFile("targetfiltering/content.xml");
        IMetadataRepository testDataRepository = metadataManager.loadRepository(testDataFile.getParentFile().toURI(),
                null);
        return testDataRepository.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet();
    }

    @Test
    public void testNoFilters() {
        subject = new TargetPlatformFilterEvaluator(Collections.<TargetPlatformFilter> emptyList());

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasSize(0));
    }

    @Test
    public void testRemoveAllOfBundleId() throws Exception {
        TargetPlatformFilter removeAllFilter = removeAllFilter(ALL_MULTIVERSION_BUNDLES);
        subject = new TargetPlatformFilterEvaluator(removeAllFilter);

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("trf.bundle.multiversion_1.0.0"));
        assertThat(removedUnits(), hasItem("trf.bundle.multiversion_2.0.0"));
        assertThat(removedUnits(), hasSize(2));
    }

    @Test
    public void testRemoveAllOfUnitId() throws Exception {
        CapabilityPattern unitIdPattern = patternWithVersion(CapabilityType.P2_INSTALLABLE_UNIT, "main.product.id",
                null);
        subject = new TargetPlatformFilterEvaluator(removeAllFilter(unitIdPattern));

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("main.product.id_0.0.1.201112271438"));
        assertThat(removedUnits(), hasSize(1));
    }

    @Test
    public void testUnitAndBundleIdDistinction() throws Exception {
        // does not match because main.product.id is not a bundle
        CapabilityPattern bundleIdPattern = patternWithVersion(CapabilityType.OSGI_BUNDLE, "main.product.id", null);
        subject = new TargetPlatformFilterEvaluator(removeAllFilter(bundleIdPattern));

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasSize(0));
    }

    @Test
    public void testRestrictToExactVersion() throws Exception {
        TargetPlatformFilter versionFilter = restrictionFilter(ALL_MULTIVERSION_BUNDLES,
                patternWithVersion(null, null, "1.0.0"));
        subject = new TargetPlatformFilterEvaluator(versionFilter);

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("trf.bundle.multiversion_2.0.0"));
        assertThat(removedUnits(), hasSize(1));
    }

    @Test
    public void testRestrictToVersionRange() throws Exception {
        TargetPlatformFilter versionRangeFilter = restrictionFilter(ALL_MULTIVERSION_BUNDLES,
                patternWithVersionRange(null, null, "[1.0.0,2.0.0)"));
        subject = new TargetPlatformFilterEvaluator(versionRangeFilter);

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("trf.bundle.multiversion_2.0.0"));
        assertThat(removedUnits(), hasSize(1));
    }

    @Test
    public void testRestrictPackageProvider() throws Exception {
        TargetPlatformFilter providerFilter = restrictionFilter(
                patternWithoutVersion(CapabilityType.JAVA_PACKAGE, "javax.persistence"),
                patternWithoutVersion(CapabilityType.OSGI_BUNDLE, "javax.persistence"));
        subject = new TargetPlatformFilterEvaluator(providerFilter);

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("com.springsource.javax.persistence_1.0.0"));
        assertThat(removedUnits(), hasSize(1));
    }

    @Test
    public void testRestrictPackageVersionInShortNotation() throws Exception {
        TargetPlatformFilter packageVersionFilter = restrictionFilter(
                patternWithoutVersion(CapabilityType.JAVA_PACKAGE, "javax.persistence"),
                patternWithVersion(null, null, "1.0.0")); // inherit attributes from scope pattern
        subject = new TargetPlatformFilterEvaluator(packageVersionFilter);

        subject.filterUnits(workUnits);

        assertThat(removedUnits(), hasItem("javax.persistence_2.0.3.v201010191057")); // provides *a* package in version 1.0.0, but not the package javax.persistence
        assertThat(removedUnits(), hasSize(1));
    }

    @Ignore("TODO")
    @Test
    public void testWarningIfRestrictionRemovesAll() throws Exception {

    }

    @Test(expected = TargetPlatformFilterSyntaxException.class)
    public void testNonParsableVersion() throws Exception {
        TargetPlatformFilter invalidFilter = restrictionFilter(ALL_MULTIVERSION_BUNDLES,
                patternWithVersion(null, null, "1.a"));
        subject = new TargetPlatformFilterEvaluator(invalidFilter);

        subject.filterUnits(workUnits);
    }

    @Test(expected = TargetPlatformFilterSyntaxException.class)
    public void testNonParsableVersionRange() throws Exception {
        TargetPlatformFilter invalidFilter = restrictionFilter(ALL_MULTIVERSION_BUNDLES,
                patternWithVersionRange(null, null, "[1.0.0,")); // "[1.0.0," is invalid; "1.0.0" is the range from 1 to infinity
        subject = new TargetPlatformFilterEvaluator(invalidFilter);

        subject.filterUnits(workUnits);
    }

    private Collection<String> removedUnits() {
        HashSet<String> result = new HashSet<String>();
        for (IInstallableUnit unit : baselineUnits) {
            if (!workUnits.contains(unit)) {
                result.add(unit.getId() + "_" + unit.getVersion());
            }
        }
        return result;
    }

    static Matcher<Collection<?>> hasSize(final int expectedSize) {
        return new TypeSafeMatcher<Collection<?>>() {
            @Override
            public boolean matchesSafely(Collection<?> collection) {
                return expectedSize == collection.size();
            }

            public void describeTo(Description description) {
                description.appendText("a collection of size ").appendValue(expectedSize);
            }
        };
    }

}
