/*******************************************************************************
 * Copyright (c) 2019 Bachmann electronic GmbH and others.
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

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JUnit4Test {

    @Test
    public void testWithJUnit4() {
        assertEquals(3, 1 + 2);
    }
}
