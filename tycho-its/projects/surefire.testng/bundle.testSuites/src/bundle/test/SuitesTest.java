/*******************************************************************************
 * Copyright (c) 2016 Bachmann electronic GmbH. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
