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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.tycho.versions.engine.DefaultVersionRangeUpdateStrategy;
import org.junit.Test;

public class DefaultVersionRangeUpdateStrategyTest {

    private DefaultVersionRangeUpdateStrategy defaultStrategy = new DefaultVersionRangeUpdateStrategy(false);

    private DefaultVersionRangeUpdateStrategy updatingMatchingBoundsStrategy = new DefaultVersionRangeUpdateStrategy(
            true);

    @Test
    public void nullShouldRemainNull() {
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

}
