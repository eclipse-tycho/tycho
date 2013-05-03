/*******************************************************************************
 * Copyright (c) 2012, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import org.eclipse.core.runtime.IStatus;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class StatusMatchers {

    public static Matcher<IStatus> errorStatus() {
        return new TypeSafeMatcher<IStatus>() {

            public void describeTo(Description description) {
                description.appendText("a status with severity ERROR");
            }

            @Override
            public boolean matchesSafely(IStatus item) {
                return item.matches(IStatus.ERROR);
            }
        };
    }

    public static Matcher<IStatus> warningStatus() {
        return new TypeSafeMatcher<IStatus>() {

            public void describeTo(Description description) {
                description.appendText("a status with severity WARNING");
            }

            @Override
            public boolean matchesSafely(IStatus item) {
                return item.matches(IStatus.WARNING);
            }
        };
    }

    public static Matcher<IStatus> okStatus() {
        return new TypeSafeMatcher<IStatus>() {

            public void describeTo(Description description) {
                description.appendText("a status with severity OK");
            }

            @Override
            public boolean matchesSafely(IStatus item) {
                return item.isOK();
            }
        };
    }

    public static Matcher<IStatus> statusWithMessageWhich(final Matcher<String> messageMatcher) {
        return new TypeSafeMatcher<IStatus>() {

            public void describeTo(Description description) {
                description.appendText("a status with a message which is ");
                description.appendDescriptionOf(messageMatcher);
            }

            @Override
            public boolean matchesSafely(IStatus item) {
                return messageMatcher.matches(item.getMessage());
            }
        };
    }

}
