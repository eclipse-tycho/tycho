/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour  - create test for indirect loading of class/resource
 *******************************************************************************/
package org.eclipse.tycho.debugtychosurefire.bundletest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.tycho.debugtychosurefire.bundle.HelloWorld;

public class HelloWorldTest
{

    @Test
    public void test()
    {
        final HelloWorld hello = new HelloWorld();
        assertTrue("Hello World".equals(hello.test()));
    }

}
