/*******************************************************************************
 * Copyright (c) 2015, 2020 VDS Rail and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Enrico De Fent - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PackageNameMatcherTest {

    @Test
    public void testNoPattern() {
        assertNotNull(PackageNameMatcher.compile(new ArrayList<String>()));
    }

    @Test
    public void testPattern01() {
        PackageNameMatcher matcher = PackageNameMatcher.compile(asList("com.example"));
        assertNotNull(matcher);
        assertTrue(matcher.matches("com.example"));
        assertFalse(matcher.matches("com.example.child"));
        assertFalse(matcher.matches("parent.com.example"));
    }

    @Test
    public void testPattern02() {
        PackageNameMatcher matcher = PackageNameMatcher.compile(asList("com.example.*"));
        assertFalse(matcher.matches("com.example"));
        assertTrue(matcher.matches("com.example.child"));
        assertTrue(matcher.matches("com.example.child.granchild"));
        assertFalse(matcher.matches("parent.com.example"));
    }

    @Test
    public void testPattern03() {
        PackageNameMatcher matcher = PackageNameMatcher.compile(asList("*.example"));
        assertTrue(matcher.matches("com.example"));
        assertTrue(matcher.matches("org.example"));
        assertTrue(matcher.matches("org.internal.example"));
        assertFalse(matcher.matches("com.example.child"));
    }

    @Test
    public void testPattern04() {
        PackageNameMatcher matcher = PackageNameMatcher.compile(asList("*.example*"));
        assertTrue(matcher.matches("com.example"));
        assertTrue(matcher.matches("org.example.pkg"));
        assertTrue(matcher.matches("org.internal.examples"));
        assertFalse(matcher.matches("example.child"));
    }

    @Test
    public void testPattern05() {
        final List<String> specs = List.of("com.example.*", "org.example.*");
        PackageNameMatcher matcher = PackageNameMatcher.compile(specs);
        assertFalse(matcher.matches("com.example"));
        assertTrue(matcher.matches("com.example.pkg"));
        assertTrue(matcher.matches("com.example.pkg.sub"));
        assertFalse(matcher.matches("org.example"));
        assertTrue(matcher.matches("org.example.pkg"));
        assertTrue(matcher.matches("org.example.pkg.sub"));
    }

    @Test
    public void testPattern06() {
        assertThrows(IllegalArgumentException.class, () -> PackageNameMatcher.compile(asList("com.example.!")));
    }

    @Test
    public void testPattern07() {
        final List<String> specs = List.of("com.example.*", "org.example.!");
        assertThrows(IllegalArgumentException.class, () -> PackageNameMatcher.compile(specs));
    }

    @Test
    public void testSpacesPattern01() {
        assertThrows(IllegalArgumentException.class, () -> PackageNameMatcher.compile(asList("  ")));
    }

    @Test
    public void testSpacesPattern02() {
        assertThrows(IllegalArgumentException.class, () -> PackageNameMatcher.compile(asList("\t\t")));
    }

}
