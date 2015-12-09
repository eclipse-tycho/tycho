package org.eclipse.tycho.versions.engine.tests;

import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_COMPATIBLE;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_EQUIVALENT;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_GREATER_OR_EQUAL;
import static org.eclipse.tycho.versions.engine.ImportRefVersionConstraint.MATCH_PERFECT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.tycho.versions.engine.ImportRefVersionConstraint;
import org.junit.Test;

public class ImportRefVersionConstraintTest {

    @Test
    public void shouldMatchAnythingWhenNullVersion() {

        ImportRefVersionConstraint rangeWithoutVersion = new ImportRefVersionConstraint(null, MATCH_PERFECT);
        assertTrue(rangeWithoutVersion.matches("0.1.0"));
        assertTrue(rangeWithoutVersion.matches("45.0.0"));
    }

    @Test
    public void testPerfectMatch() {
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_PERFECT).matches("0.1.0"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_PERFECT).matches("0.1.0.a"));

        assertFalse(new ImportRefVersionConstraint("0.1.1", MATCH_PERFECT).matches("0.1.0"));
        assertFalse(new ImportRefVersionConstraint("0.1.0.a", MATCH_PERFECT).matches("0.1.0.b"));
    }

    @Test
    public void testEquivalentMatch() {
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_EQUIVALENT).matches("0.1.0"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_EQUIVALENT).matches("0.1.0.a"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_EQUIVALENT).matches("0.1.0.b"));
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_EQUIVALENT).matches("0.1.1"));

        assertFalse(new ImportRefVersionConstraint("0.1.0", MATCH_EQUIVALENT).matches("0.2.0"));
        assertFalse(new ImportRefVersionConstraint("0.1.0", MATCH_EQUIVALENT).matches("1.1.0"));

        assertFalse(new ImportRefVersionConstraint("0.1.1", MATCH_EQUIVALENT).matches("0.1.0"));
        assertFalse(new ImportRefVersionConstraint("0.1.0.b", MATCH_EQUIVALENT).matches("0.1.0.a"));

    }

    @Test
    public void testCompatibleMatch() {
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_COMPATIBLE).matches("0.1.0"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_COMPATIBLE).matches("0.1.0.a"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_COMPATIBLE).matches("0.1.0.b"));
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_COMPATIBLE).matches("0.1.1"));
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_COMPATIBLE).matches("0.2.0"));

        assertFalse(new ImportRefVersionConstraint("0.1.0", MATCH_COMPATIBLE).matches("1.0.0"));

        assertFalse(new ImportRefVersionConstraint("0.1.1", MATCH_COMPATIBLE).matches("0.1.0"));
        assertFalse(new ImportRefVersionConstraint("0.1.0.b", MATCH_COMPATIBLE).matches("0.1.0.a"));

    }

    @Test
    public void testGreaterOrEqualMatch() {
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_GREATER_OR_EQUAL).matches("0.1.0"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_GREATER_OR_EQUAL).matches("0.1.0.a"));
        assertTrue(new ImportRefVersionConstraint("0.1.0.a", MATCH_GREATER_OR_EQUAL).matches("0.1.0.b"));
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_GREATER_OR_EQUAL).matches("0.1.1"));
        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_GREATER_OR_EQUAL).matches("0.2.0"));

        assertTrue(new ImportRefVersionConstraint("0.1.0", MATCH_GREATER_OR_EQUAL).matches("1.0.0"));

        assertFalse(new ImportRefVersionConstraint("0.1.1", MATCH_GREATER_OR_EQUAL).matches("0.1.0"));
        assertFalse(new ImportRefVersionConstraint("0.1.0.b", MATCH_GREATER_OR_EQUAL).matches("0.1.0.a"));

    }
}
