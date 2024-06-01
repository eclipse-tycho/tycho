/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JUnit59Test {

    @Test
    @DisplayName("started from test suite")
    void startedFromSuite() {
        assertEquals(2, 1 + 1);
    }

}
