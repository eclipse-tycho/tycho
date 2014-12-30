/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.testutil;

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
