package tycho.its.uiharness;

import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;

/**
 * Not executed by this integration test, the configured application does not exist, but the test
 * runtime must contain at least one test class to be launched at all.
 */
public class CustomApplicationTest {

	@Test
	public void platformIsRunning() {
		assertNotNull(Platform.getBundle("org.eclipse.core.runtime"));
	}
}
