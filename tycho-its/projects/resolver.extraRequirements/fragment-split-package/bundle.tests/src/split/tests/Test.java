package split.tests;

import split.FragmentClass;
import junit.framework.TestCase;

public class Test
    extends TestCase
{

    public void test()
    {
        assertEquals( "1", new FragmentClass().getValue() );
    }
}
