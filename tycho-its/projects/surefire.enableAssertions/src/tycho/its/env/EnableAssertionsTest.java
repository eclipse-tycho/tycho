/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
