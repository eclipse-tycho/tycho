/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Bachmann electronic GmbH. - initial API and implementation
 *******************************************************************************/
package bundle.test;

import org.testng.annotations.Test;

public class SuitesTest {

    @Test
    public void anEnabledTestMethod() {
        assert true;
    }
    
    @Test
    public void anotherEnabledTestMethod() {
        assert true;
    }
    
    @Test
    public void dissabledTestMethod() {
       assert false: "This test must not be executed because it's excluded in the suite xml";
    }
}
