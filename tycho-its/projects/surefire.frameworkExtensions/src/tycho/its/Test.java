/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package tycho.its;

import junit.framework.TestCase;

public class Test
    extends TestCase
{

    public void test()
        throws Exception
    {
        assertEquals( "PASSED", System.getProperty( "tycho.353" ) );
        assertNotNull( getClass().getResource( "/tycho353/tycho353.properties" ) );
    }

}
