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
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.SubstringMatcher;
import org.junit.matchers.JUnitMatchers;

/**
 * Additional {@link Matcher}s for assertions in Tycho's tests.
 * 
 * @see CoreMatchers
 * @see JUnitMatchers
 */
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
     * Returns a matcher matching any list that contains the given sequence of elements.
     * 
     * @see JUnitMatchers#hasItem(Matcher)
     */
    public static <T> Matcher<List<T>> hasSequence(final T... sequence) {
        if (sequence.length == 0) {
            throw new IllegalArgumentException();
        }

        return new TypeSafeMatcher<List<T>>() {

            public void describeTo(Description description) {
                description.appendValueList("a list with the sequence ", ", ", "", sequence);
            }

            @Override
            public boolean matchesSafely(List<T> actualList) {
                for (int actualListIx = 0; actualListIx <= actualList.size() - sequence.length; ++actualListIx) {
                    if (sequenceMatches(actualList, actualListIx, sequence)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean sequenceMatches(List<T> actualList, int actualListIx, final T[] sequence) {
                for (int sequenceIx = 0; sequenceIx < sequence.length; ++sequenceIx) {
                    if (!(sequence[sequenceIx].equals(actualList.get(actualListIx + sequenceIx))))
                        return false;
                }
                return true;
            }
        };
    }

    /**
     * Returns a matcher matching any existing file or directory.
     */
    public static Matcher<File> exists() {
        return new TypeSafeMatcher<File>() {

            public void describeTo(Description description) {
                description.appendText("an existing file or directory");
            }

            @Override
            public boolean matchesSafely(File item) {
                return item.exists();
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
