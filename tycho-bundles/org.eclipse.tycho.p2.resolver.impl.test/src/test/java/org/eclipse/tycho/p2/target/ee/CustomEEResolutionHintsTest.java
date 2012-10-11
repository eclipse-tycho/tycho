/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.p2.target.ee;

import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.test.util.InstallableUnitUtil;
import org.junit.Test;

@SuppressWarnings("restriction")
public class CustomEEResolutionHintsTest {

    private static final IInstallableUnit CUSTOM_PROFILE_IU = createCandidateIU("a.jre.custom", "1.1.0");
    private static final IInstallableUnit CUSTOM_PROFILE_IU_OTHER_VERSION = createCandidateIU("a.jre.custom", "1.6");
    private static final IInstallableUnit VIRGO_PROFILE_IU = createCandidateIU("a.jre.virgo.javase", "1.6");

    private CustomEEResolutionHints subject;

    @Test
    public void testJREUnitNameForSimpleProfileName() {
        subject = new CustomEEResolutionHints("Custom-1.1");

        assertTrue(subject.isEESpecificationUnit(CUSTOM_PROFILE_IU));
        assertFalse(subject.isEESpecificationUnit(CUSTOM_PROFILE_IU_OTHER_VERSION));
        assertFalse(subject.isEESpecificationUnit(VIRGO_PROFILE_IU));
    }

    @Test
    public void testJREUnitNameForComplexProfileName() {
        subject = new CustomEEResolutionHints("Virgo/JavaSE-1.6");

        assertTrue(subject.isEESpecificationUnit(VIRGO_PROFILE_IU));
        assertFalse(subject.isEESpecificationUnit(CUSTOM_PROFILE_IU));
        assertFalse(subject.isEESpecificationUnit(CUSTOM_PROFILE_IU_OTHER_VERSION));
    }

    @Test(expected = InvalidEENameException.class)
    public void testProfileNameWithoutVersion() {
        subject = new CustomEEResolutionHints("Virgo/Java6");
    }

    @Test(expected = InvalidEENameException.class)
    public void testProfileNameWithInvalidVersion() {
        subject = new CustomEEResolutionHints("Virgo/Java-1.6a");
    }

    @Test
    public void testConsistencyWithJREAction() throws Exception {
        JREAction jreAction = new JREAction(resourceFile("profiles/TestMe-1.8.profile"));
        PublisherResult results = new PublisherResult();
        jreAction.perform(new PublisherInfo(), results, null);
        Set<IInstallableUnit> publishedUnits = results.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet();

        subject = new CustomEEResolutionHints("TestMe-1.8");

        for (IInstallableUnit unit : publishedUnits) {
            if (subject.isEESpecificationUnit(unit)) {
                // success: we find the unit create by the JREAction
                return;
            }
        }
        fail("'a.jre' unit for profile 'TestMe-1.8' not found in" + publishedUnits);
    }

    private static IInstallableUnit createCandidateIU(String unitId, String version) {
        return InstallableUnitUtil.createIU(unitId, version);
    }

}
