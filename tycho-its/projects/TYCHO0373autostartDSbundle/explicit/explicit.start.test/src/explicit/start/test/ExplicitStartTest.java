/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package explicit.start.test;

import org.junit.Assert;
import org.junit.Test;

import explicit.start.Activator;

public class ExplicitStartTest
{
    @Test
    public void test()
    {
        Assert.assertNotNull( Activator.context );
    }
}
