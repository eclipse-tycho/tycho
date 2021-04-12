/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Additional {@link Matcher}s for assertions in Tycho's tests.
 * 
 * @see CoreMatchers
 */
public class TychoMatchers {

    /**
     * Creates a matcher matching any list that contains the given sequence of elements.
     * 
     * @see CoreMatchers#hasItem(Matcher)
     */
    public static <T> Matcher<List<T>> hasSequence(final T... sequence) {
        if (sequence.length == 0) {
            throw new IllegalArgumentException();
        }

        return new TypeSafeMatcher<List<T>>() {

            @Override
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
     * Creates a matcher matching any collection with the given size.
     * 
     * @see CoreMatchers#hasItem(Matcher)
     */
    public static <T> Matcher<Collection<? extends T>> hasSize(final int size) {
        return new TypeSafeMatcher<Collection<? extends T>>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("a collection with size " + size);
            }

            @Override
            protected boolean matchesSafely(Collection<? extends T> item) {
                return item.size() == size;
            }
        };
    }

    /**
     * Returns a matcher matching any existing file or directory.
     */
    public static Matcher<File> exists() {
        return new TypeSafeMatcher<File>() {

            @Override
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

            @Override
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
