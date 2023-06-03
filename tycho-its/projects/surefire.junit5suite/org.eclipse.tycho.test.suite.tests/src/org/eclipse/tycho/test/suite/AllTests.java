package org.eclipse.tycho.test.suite;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SelectClasses({
    FooTest1.class,
    FooTest2.class
})

@SuiteDisplayName("All Tests")
public class AllTests {
}