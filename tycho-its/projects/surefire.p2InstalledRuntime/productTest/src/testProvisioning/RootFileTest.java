package testProvisioning;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

public class RootFileTest extends TestCase {

	public void testIsProvisionedInstallation() throws Exception {
		File eclipseInstallationRoot = new File(FileLocator.toFileURL(
				Platform.getInstallLocation().getURL()).getPath());
		File rootFile = new File(eclipseInstallationRoot, "README.txt");
		assertTrue(rootFile + " does not exist - installation was not provisioned by p2", rootFile.isFile());
	}

}
