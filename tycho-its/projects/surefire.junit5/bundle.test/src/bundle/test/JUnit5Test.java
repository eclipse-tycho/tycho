/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE- initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class JUnit5Test {

    @Test
    @DisplayName("My 1st JUnit 5 test!")
    void myFirstJUnit5Test(TestInfo testInfo) {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
        assertEquals("My 1st JUnit 5 test!", testInfo.getDisplayName(), () -> "TestInfo is injected correctly");
    }

    @Test
    @Tag("slow")
    void slowJUnit5Test() {
        assertEquals(2, 1+1, "1 + 1 should equal 2");
    }

}
