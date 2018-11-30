package com.stegmannsystems.debugtychosurefire.bundletest;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.stegmannsystems.debugtychosurefire.bundle.HelloWorld;

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
