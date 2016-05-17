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

public class GroupsTest {

    @Test(groups = {"executeMe1"})
    public void anEnabledTestMethod() {
        assert true;
    }
    
    @Test(groups = {"executeMe2"})
    public void anotherEnabledTestMethod() {
        assert true;
    }
    
    @Test(groups = {"doNotExecuteMe"})
    public void dissabledTestMethod() {
       assert false: "This test must not be executed because it's group is not specified in the pom";
    }
}
