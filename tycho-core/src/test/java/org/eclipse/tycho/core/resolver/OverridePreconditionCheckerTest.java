/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenDependency;
import org.eclipse.tycho.targetplatform.TargetDefinition.MavenGAVLocation.DependencyDepth;
import org.junit.Test;

public class OverridePreconditionCheckerTest {

    private static final String ORIGINAL = "com.example.original";
    private static final String OVERRIDE = "com.example.override";
    private static final Collection<MavenDependency> ROOTS = Collections.singletonList(null);

    @Test
    public void testDependencyDepthDiretIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(OVERRIDE),
                DependencyDepth.DIRECT, ROOTS);
        assertEquals("The dependency depth must be none!", result);
    }

    @Test
    public void testDependencyDepthNullIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(OVERRIDE),
                null, ROOTS);
        assertEquals("The dependency depth must be none!", result);
    }

    @Test
    public void testOriginalSymbolicNameNullIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(null, singleInstruction(OVERRIDE),
                DependencyDepth.NONE, ROOTS);
        assertEquals("The artifact is no bundle.", result);
    }

    @Test
    public void testRootsEmptyIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(OVERRIDE),
                DependencyDepth.NONE, Collections.emptyList());
        assertEquals("The location must contain exactly one root dependency.", result);
    }

    @Test
    public void testRootsMultipleIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(OVERRIDE),
                DependencyDepth.NONE, Arrays.asList(new TestMavenDependency(), new TestMavenDependency()));
        assertEquals("The location must contain exactly one root dependency.", result);
    }

    @Test
    public void testInstructionsMapEmptyIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, Map.of(), DependencyDepth.NONE,
                ROOTS);
        assertEquals(
                "The location must contain exactly one bnd instruction which must contain a symbolic name that differs from the original one.",
                result);
    }

    @Test
    public void testInstructionsMapWithMultipleEntriesIsRejected() {
        Map<String, Properties> instructions = new HashMap<>();
        instructions.put("a", propertiesWithSymbolicName(OVERRIDE));
        instructions.put("b", propertiesWithSymbolicName("com.example.other"));
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, instructions,
                DependencyDepth.NONE, ROOTS);
        assertEquals(
                "The location must contain exactly one bnd instruction which must contain a symbolic name that differs from the original one.",
                result);
    }

    @Test
    public void testNullPropertiesIsRejected() {
        Map<String, Properties> instructions = new HashMap<>();
        instructions.put("a", null);
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, instructions,
                DependencyDepth.NONE, ROOTS);
        assertEquals(" The symbolic name in the bnd instructions must be defined and differ from the original one.",
                result);
    }

    @Test
    public void testMissingSymbolicNameIsRejected() {
        Map<String, Properties> instructions = new HashMap<>();
        instructions.put("a", new Properties());
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, instructions,
                DependencyDepth.NONE, ROOTS);
        assertEquals(" The symbolic name in the bnd instructions must be defined and differ from the original one.",
                result);
    }

    @Test
    public void testSameSymbolicNameIsRejected() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(ORIGINAL),
                DependencyDepth.NONE, ROOTS);
        assertEquals(" The symbolic name in the bnd instructions must be defined and differ from the original one.",
                result);
    }

    @Test
    public void testDifferentSymbolicNameIsAccepted() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, singleInstruction(OVERRIDE),
                DependencyDepth.NONE, ROOTS);
        assertNull(result);
    }

    @Test
    public void testSymbolicNameWithWhitespaceIsAccepted() {
        String result = OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL,
                singleInstruction("  " + OVERRIDE + "  "), DependencyDepth.NONE, ROOTS);
        assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testNullInstructionsMapThrows() {
        OverridePreconditionChecker.checkOverridePreconditions(ORIGINAL, null, DependencyDepth.NONE, ROOTS);
    }

    private static Map<String, Properties> singleInstruction(String symbolicName) {
        Map<String, Properties> instructions = new HashMap<>();
        instructions.put("bnd", propertiesWithSymbolicName(symbolicName));
        return instructions;
    }

    private static Properties propertiesWithSymbolicName(String symbolicName) {
        Properties properties = new Properties();
        properties.put("Bundle-SymbolicName", symbolicName);
        return properties;
    }

    private static class TestMavenDependency implements MavenDependency {

        @Override
        public String getGroupId() {
            return null;
        }

        @Override
        public String getArtifactId() {
            return null;
        }

        @Override
        public String getVersion() {
            return null;
        }

        @Override
        public String getArtifactType() {
            return null;
        }

        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public boolean isIgnored(IArtifactFacade artifact) {
            return false;
        }

    }
}
