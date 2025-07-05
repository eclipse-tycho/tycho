package osgitest;

import org.osgi.framework.BundleContext;

public class TestMe {

	private BundleContext bc;

	public TestMe(BundleContext bc) {
		this.bc = bc;
	}

	public String doIt() {
		return bc.getBundles().length + " Bundles are in your framework!";
	}
}
