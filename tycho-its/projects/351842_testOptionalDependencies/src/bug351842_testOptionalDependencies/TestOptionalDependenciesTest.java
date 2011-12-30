package bug351842_testOptionalDependencies;

import org.eclipse.core.runtime.Platform;

import junit.framework.TestCase;

public class TestOptionalDependenciesTest extends TestCase {
    public void test() throws Exception {
        // the point of this test is to assert that mutually exclusive optional dependencies
        // can be excluded with optionalDependencies=ignore

        assertNull(Platform.getBundle("org.eclipse.equinox.frameworkadmin"));
    }
}
