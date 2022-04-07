/*******************************************************************************
 * Copyright (c) 2015, 2019 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
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
