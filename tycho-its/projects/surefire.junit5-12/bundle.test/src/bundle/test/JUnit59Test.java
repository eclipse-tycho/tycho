/*******************************************************************************
 * Copyright (c) 2018 Bachmann electronic GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JUnit59Test {

    @Test
    @DisplayName("My 1st JUnit 5.9 test!")
    void myFirstJUnit59Test(TestInfo testInfo) {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
        assertEquals("My 1st JUnit 5.9 test!", testInfo.getDisplayName(), () -> "TestInfo is injected correctly");
    }

    @Test
    @Tag("slow")
    void slowJUnit5Test() {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
    }

    private static Stream<Arguments> testData() {
        return Stream.of(Arguments.arguments(0, 5, 5), Arguments.arguments(10, 10, 20), Arguments.arguments(12, 30, 42));
    }

    @ParameterizedTest
    @MethodSource("testData")
    void parameterizedJUnit59TestWithMethodSource(int number1, int number2, int expectedSum) {
        assertEquals(expectedSum, number1 + number2, "number1 + number2 should be same as expected sum");
    }

}
