/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.its;

import java.io.File;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TychoMatchers {

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
