package phf.consumer;

import junit.framework.TestCase;
import phf.host.HostInterfaceFactory;

public class HostInterfaceUsageTest extends TestCase {

    public void testHostInterfaceImplementationIsAvailable() throws Exception {
        assertEquals("Result from org.example.phf.linux.gtk.x86_64", HostInterfaceFactory.newInstance()
                .getPlatformFunctionality());
    }

}
