/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour  - create test for indirect loading of class/resource
 *******************************************************************************/
package org.eclipse.tycho.debugtychosurefire.bundletest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.tycho.debugtychosurefire.bundle.HelloWorld;

public class HelloWorldTest
{

    @Test
    public void test()
    {
        final HelloWorld hello = new HelloWorld();
        assertEquals("Hello World", hello.test());
    }

}
