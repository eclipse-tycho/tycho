package surefire.runorder;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BTest
{

    public static boolean testWasRun = false;
    
    @Test
    public void testB()
    {
        assertTrue( true );
    }
    
    @BeforeClass
    public static void reset() {
        testWasRun = false;
    }
    
    @AfterClass
    public static void markAsRun() {
        testWasRun = true;
    }

}
