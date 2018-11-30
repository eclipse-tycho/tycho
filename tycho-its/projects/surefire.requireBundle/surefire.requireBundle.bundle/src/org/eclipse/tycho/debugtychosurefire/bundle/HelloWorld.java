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
package org.eclipse.tycho.debugtychosurefire.bundle;

import static org.junit.Assert.fail;

import javax.xml.validation.SchemaFactory;

public class HelloWorld
{

    public String test()
    {
        final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
        if (sf == null) {
            fail("SchemaFactory not found");
            return "SchemaFactory not found";
        }
        return "Hello World";
    }

}
