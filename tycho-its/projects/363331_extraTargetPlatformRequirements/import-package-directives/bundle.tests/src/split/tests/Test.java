package split.tests;

import junit.framework.TestCase;
import split.BundleAClass;
import unsplit.BundleBClass;

public class Test
    extends TestCase
{
    public void test()
    {
        assertEquals( "A", new BundleAClass().getValue() );
        assertEquals( "B", new BundleBClass().getValue() );
    }
}
