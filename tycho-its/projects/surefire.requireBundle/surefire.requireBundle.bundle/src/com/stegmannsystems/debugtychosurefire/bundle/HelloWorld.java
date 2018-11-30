/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour  - create test for indirect loading of class/resource
 *******************************************************************************/
package com.stegmannsystems.debugtychosurefire.bundle;

import javax.xml.validation.SchemaFactory;

/**
 * @author schulz
 */
public class HelloWorld
{

    public String test()
    {
        final SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
        if (sf == null) {
            // fail
        }
        return "Hello World";
    }

}
