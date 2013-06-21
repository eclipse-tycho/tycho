package surefire.runorder;

import static org.junit.Assert.*;

import org.junit.Test;

public class ATest
{

    @Test
    public void testA()
    {
        assertTrue("BTest should have been executed before this test", BTest.testWasRun);
    }

}
