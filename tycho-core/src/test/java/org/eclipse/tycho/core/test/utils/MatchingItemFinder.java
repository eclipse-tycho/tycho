/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
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
package org.eclipse.tycho.core.test.utils;

import static org.junit.Assert.fail;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public class MatchingItemFinder {

    public static <T> T getUnique(Matcher<T> condition, Iterable<? extends T> collection) {
        T result = null;
        for (T item : collection) {
            if (condition.matches(item)) {
                if (result == null)
                    result = item;
                else {
                    reportMismatch(condition, collection);
                }
            }
        }
        if (result == null) {
            reportMismatch(condition, collection);
        }
        return result;
    }

    private static <T> void reportMismatch(Matcher<T> condition, Iterable<? extends T> collection) {
        Description description = new StringDescription();
        description.appendText("Expected a collection with exactly one item: ").appendDescriptionOf(condition);
        description.appendText("\nbut was: ").appendValue(collection);
        fail(description.toString());
    }
}
