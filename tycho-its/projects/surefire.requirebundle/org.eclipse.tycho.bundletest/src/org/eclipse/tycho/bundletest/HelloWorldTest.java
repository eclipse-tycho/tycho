package org.eclipse.tycho.bundletest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.tycho.bundle.HelloWorld;

/**
 * @author schulz
 */
public class HelloWorldTest
{

    @Test
    public void test()
    {
        final HelloWorld hello = new HelloWorld();
        assertTrue(hello.test().equals("Hello World"));
    }

}
