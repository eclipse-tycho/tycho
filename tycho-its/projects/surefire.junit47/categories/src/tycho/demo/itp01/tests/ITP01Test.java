package tycho.demo.itp01.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ITP01Test {

    @Test
    public void testNoCategories() {
      Assert.fail();
    }
    
    @Test
    @Category(SlowTests.class)
    public void testSlow() {
        Assert.fail();

    }
    @Test
    @Category(FastTests.class)
    public void testFast() {
        Assert.assertTrue(true);
    }

}
