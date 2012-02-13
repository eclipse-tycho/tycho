package test;

import org.osgi.framework.Version;

import junit.framework.TestCase;

public class Test
    extends TestCase
{
    public void test()
    {
        Version version = new Version( (String) Activator.context.getBundle( 0 ).getHeaders().get( "Bundle-Version" ) );

        assertTrue( version.compareTo( new Version( "3.5" ) ) < 0 );
    }
}
