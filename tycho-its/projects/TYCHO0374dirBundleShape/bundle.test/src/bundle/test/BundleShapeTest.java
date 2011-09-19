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
package bundle.test;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleShapeTest
{
    @Test
    public void test()
        throws Exception
    {
        Bundle bundle = getBundle( "bundle" );

        URL entry = FileLocator.resolve( bundle.getEntry( "data/file.txt" ) );

        Assert.assertEquals( "file", entry.getProtocol() );
    }

    public Bundle getBundle( String id )
    {
        for ( Bundle bundle : Activator.context.getBundles() )
        {
            if ( id.equals( bundle.getSymbolicName() ) )
            {
                return bundle;
            }
        }

        throw new IllegalStateException();
    }
}
