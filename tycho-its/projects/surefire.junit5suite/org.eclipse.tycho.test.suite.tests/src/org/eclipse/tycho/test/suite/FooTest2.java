package org.eclipse.tycho.test.suite;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class FooTest2 {

    @Test
    public void testFoo2() {
        Foo foo = new Foo();
        assertNotNull(foo);
    }
}
