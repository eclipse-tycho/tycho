/*******************************************************************************
 * Copyright (c) 2019 Bachmann electronic GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
