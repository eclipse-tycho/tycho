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
package bundle;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import test.component.IComponent;

public class AutostartTest
{
    @Test
    public void test()
    {
        ServiceReference serviceReference = Activator.context.getServiceReference( IComponent.class.getName() );
        Assert.assertNotNull( serviceReference );
    }
}
