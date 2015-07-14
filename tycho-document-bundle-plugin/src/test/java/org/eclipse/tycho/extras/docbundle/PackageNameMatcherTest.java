/*******************************************************************************
 * Copyright (c) 2015 VDS Rail and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Enrico De Fent - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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
        final List<String> specs = new ArrayList<>();
        specs.add("com.example.*");
        specs.add("org.example.*");
        PackageNameMatcher matcher = PackageNameMatcher.compile(specs);
        assertFalse(matcher.matches("com.example"));
        assertTrue(matcher.matches("com.example.pkg"));
        assertTrue(matcher.matches("com.example.pkg.sub"));
        assertFalse(matcher.matches("org.example"));
        assertTrue(matcher.matches("org.example.pkg"));
        assertTrue(matcher.matches("org.example.pkg.sub"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPattern06() {
        PackageNameMatcher.compile(asList("com.example.!"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPattern07() {
        final List<String> specs = new ArrayList<>();
        specs.add("com.example.*");
        specs.add("org.example.!");
        PackageNameMatcher.compile(specs);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpacesPattern01() {
        PackageNameMatcher.compile(asList("  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpacesPattern02() {
        PackageNameMatcher.compile(asList("\t\t"));
    }

}
