package tycho.testsun;

import java.security.Security;

public class TestClass {
    public TestClass() {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

    }
}
