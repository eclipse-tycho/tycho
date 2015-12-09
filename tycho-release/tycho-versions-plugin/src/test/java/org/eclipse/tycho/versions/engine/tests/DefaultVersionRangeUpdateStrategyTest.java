/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Arod - initial implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine.tests;

import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_COMPATIBLE;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_EQUIVALENT;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_GREATER_OR_EQUAL;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_PERFECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.tycho.versions.engine.DefaultVersionRangeUpdateStrategy;
import org.eclipse.tycho.versions.engine.ImportRefVersionConstraint;
import org.junit.Test;

public class DefaultVersionRangeUpdateStrategyTest {

    private DefaultVersionRangeUpdateStrategy defaultStrategy = new DefaultVersionRangeUpdateStrategy(false);

    private DefaultVersionRangeUpdateStrategy updatingMatchingBoundsStrategy = new DefaultVersionRangeUpdateStrategy(
            true);

    @Test
    public void nullVersionRangeShouldRemainNull() {
        assertNull(defaultStrategy.computeNewVersionRange(null, "1.0.0", "1.1.1"));
        assertNull(updatingMatchingBoundsStrategy.computeNewVersionRange(null, "1.0.0", "1.1.1"));
    }

    @Test
    public void rangesStillIncludingVersionShouldBeUnchanged() {
        String from = "1.0.0";
        String to = "1.1.0";

        // Equivalent to "[0.5.0,)"
        String originalRange1 = "0.5.0";
        assertEquals(originalRange1, defaultStrategy.computeNewVersionRange(originalRange1, from, to));

        String originalRange2 = "[0.5.0,1.1.0]";
        assertEquals(originalRange2, defaultStrategy.computeNewVersionRange(originalRange2, from, to));

        String originalRange3 = "[0.0.0,1.1.0]";
        assertEquals(originalRange3, defaultStrategy.computeNewVersionRange(originalRange3, from, to));
    }

    @Test
    public void rangesAlreadyExcludingVersionShouldBeUnchanged() {
        String from = "1.0.0";
        String to = "1.1.0";

        String originalRange1 = "[0.5.0,0.6.0]";
        assertEquals(originalRange1, defaultStrategy.computeNewVersionRange(originalRange1, from, to));

        String originalRange2 = "[0.5.0,1.0.0)";
        assertEquals(originalRange2, defaultStrategy.computeNewVersionRange(originalRange2, from, to));

        String originalRange3 = "1.0.1";
        assertEquals(originalRange3, defaultStrategy.computeNewVersionRange(originalRange3, from, to));
    }

    @Test
    public void rangesNowExcludingVersionShouldBeUpdatedWhenUpgradingVersion() {
        String from = "1.0.0";
        String to = "1.1.0";

        assertEquals("[1.0.0,1.1.0]", defaultStrategy.computeNewVersionRange("[1.0.0, 1.0.0]", from, to));

        assertEquals("[1.0.0,1.1.0]", defaultStrategy.computeNewVersionRange("[1.0.0, 1.1.0)", from, to));
    }

    @Test
    public void rangesNowExcludingVersionShouldBeUpdatedWhenDowngradingVersion() {
        String from = "1.1.0";
        String to = "1.0.0";

        assertEquals("1.0.0", defaultStrategy.computeNewVersionRange("1.1.0", from, to));

        assertEquals("[1.0.0,1.2.0]", defaultStrategy.computeNewVersionRange("[1.0.9, 1.2.0]", from, to));

        assertEquals("[1.0.0,1.2.0]", defaultStrategy.computeNewVersionRange("(1.0.0, 1.2.0]", from, to));
    }

    @Test
    public void boundsShouldBeUpdatedWhenMatching() {
        String from = "1.0.0";
        String to = "1.1.0";

        assertEquals("1.1.0", updatingMatchingBoundsStrategy.computeNewVersionRange("1.0.0", from, to));

        assertEquals("[0.5.0,1.1.0]", updatingMatchingBoundsStrategy.computeNewVersionRange("[0.5.0,1.0.0]", from, to));
        assertEquals("[1.1.0,2.0.0]", updatingMatchingBoundsStrategy.computeNewVersionRange("[1.0.0,2.0.0]", from, to));
        assertEquals("[1.1.0,1.1.0]", updatingMatchingBoundsStrategy.computeNewVersionRange("[1.0.0,1.0.0]", from, to));

    }

    @Test
    public void versionConstraintWithNullVersionShouldRemainNull() {
        assertNull(defaultStrategy.computeNewImportRefVersionConstraint(
                new ImportRefVersionConstraint(null, MATCH_PERFECT), "1.0.0", "1.1.0").getVersion());
        assertNull(updatingMatchingBoundsStrategy.computeNewImportRefVersionConstraint(
                new ImportRefVersionConstraint(null, MATCH_PERFECT), "1.0.0", "1.1.0").getVersion());
    }

    @Test
    public void versionConstraintsStillIncludingVersionShouldBeUnchanged() {
        String from = "1.0.0";

        ImportRefVersionConstraint originalRange1 = new ImportRefVersionConstraint(from, MATCH_EQUIVALENT);
        assertEquals(originalRange1,
                defaultStrategy.computeNewImportRefVersionConstraint(originalRange1, from, "1.0.1"));

        ImportRefVersionConstraint originalRange2 = new ImportRefVersionConstraint(from, MATCH_COMPATIBLE);
        assertEquals(originalRange2,
                defaultStrategy.computeNewImportRefVersionConstraint(originalRange2, from, "1.0.0"));

        ImportRefVersionConstraint originalRange3 = new ImportRefVersionConstraint(from, MATCH_GREATER_OR_EQUAL);
        assertEquals(originalRange3,
                defaultStrategy.computeNewImportRefVersionConstraint(originalRange3, from, "2.0.0"));

    }

    @Test
    public void versionConstraintNowExcludingVersionShouldBeUpdated() {

        assertEquals(new ImportRefVersionConstraint("1.0.1", MATCH_PERFECT),
                defaultStrategy.computeNewImportRefVersionConstraint(
                        new ImportRefVersionConstraint("1.0.0", MATCH_PERFECT), "1.0.0.qualifier", "1.0.1.qualifier"));

        assertEquals(new ImportRefVersionConstraint("1.1.0", MATCH_EQUIVALENT),
                defaultStrategy.computeNewImportRefVersionConstraint(
                        new ImportRefVersionConstraint("1.0.0", MATCH_EQUIVALENT), "1.0.0.qualifier",
                        "1.1.0.qualifier"));

        assertEquals(new ImportRefVersionConstraint("2.0.0", MATCH_COMPATIBLE),
                defaultStrategy.computeNewImportRefVersionConstraint(
                        new ImportRefVersionConstraint("1.0.0", MATCH_COMPATIBLE), "1.0.0.qualifier",
                        "2.0.0.qualifier"));

        assertEquals(new ImportRefVersionConstraint("1.0.0", MATCH_GREATER_OR_EQUAL),
                defaultStrategy.computeNewImportRefVersionConstraint(
                        new ImportRefVersionConstraint("2.0.0", MATCH_GREATER_OR_EQUAL), "2.0.0.qualifier",
                        "1.0.0.qualifier"));
    }

    @Test
    public void versionConstraintVersionShouldBeUpdatedWhenMatching() {

        assertEquals(new ImportRefVersionConstraint("1.1.0", MATCH_GREATER_OR_EQUAL),
                updatingMatchingBoundsStrategy.computeNewImportRefVersionConstraint(
                        new ImportRefVersionConstraint("1.0.0", MATCH_GREATER_OR_EQUAL), "1.0.0.qualifier",
                        "1.1.0.qualifier"));

    }

}
