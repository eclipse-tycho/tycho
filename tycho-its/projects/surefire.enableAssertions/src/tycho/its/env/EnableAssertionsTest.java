/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package tycho.its.env.vars;


import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class EnableAssertionsTest
{

    @Test
    public void testAssertionFailure()
        throws Exception
    {
        assert(1 == 2);
    }
}
