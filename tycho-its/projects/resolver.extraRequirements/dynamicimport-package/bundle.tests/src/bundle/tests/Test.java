package bundle.tests;

import junit.framework.TestCase;

public class Test
    extends TestCase
{

    public void test()
        throws Exception
    {
        assertNotNull( Class.forName( "bundle.BundleClass" ) );
    }
}
