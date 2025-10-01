/*******************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JUnit6Test {

    @Test
    @DisplayName("My 1st JUnit 6 test!")
    void myFirstJUnit6Test(TestInfo testInfo) {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
        assertEquals("My 1st JUnit 6 test!", testInfo.getDisplayName(), () -> "TestInfo is injected correctly");
    }

    @Test
    @Tag("slow")
    void slowJUnit6Test() {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
    }

    @ParameterizedTest
    @ValueSource(strings = { "one", "two" })
    void parameterizedJUnit6Test(String input) {
        assertEquals(3, input.length(), "input length should be 3");
    }

    @RepeatedTest(3)
    void repeatedJUnit6Test() {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
    }
}
