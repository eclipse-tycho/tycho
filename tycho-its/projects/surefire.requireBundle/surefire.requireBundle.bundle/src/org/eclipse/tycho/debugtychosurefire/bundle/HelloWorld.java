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
