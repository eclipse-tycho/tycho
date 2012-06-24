/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.SubstringMatcher;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.matchers.JUnitMatchers;

/**
 * Additional {@link Matcher}s for assertions in Tycho's tests.
 * 
 * @see CoreMatchers
 * @see JUnitMatchers
 */
@SuppressWarnings("restriction")
public class TychoMatchers {

    /**
     * Returns a matcher matching any string that ends in <tt>suffix</tt>.
     * 
     * @see JUnitMatchers#containsString(String)
     */
    public static Matcher<String> endsWithString(String suffix) {
        return new SubstringMatcher(suffix) {

            @Override
            protected String relationship() {
                return "ending with";
            }

            @Override
            protected boolean evalSubstringOf(String string) {
                return string.endsWith(substring);
            }
        };
    }

    /**
     * Returns a matcher matching any existing, regular file.
     */
    public static Matcher<File> isFile() {
        return new TypeSafeMatcher<File>() {

            public void describeTo(Description description) {
                description.appendText("an existing file");
            }

            @Override
            public boolean matchesSafely(File item) {
                return item.isFile();
            }
        };
    }
}
