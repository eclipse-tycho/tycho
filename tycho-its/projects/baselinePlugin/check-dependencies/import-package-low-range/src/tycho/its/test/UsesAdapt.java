package tycho.its.test;

import org.osgi.framework.Bundle;

/**
 * Uses Bundle.adapt(Class) which was added in org.osgi.framework 1.6.0.
 * The MANIFEST declares version range [1.5.0,2.0.0) which is too low.
 * Expected: range should be updated to [1.6.0,2.0.0).
 */
public class UsesAdapt {
	public Object adapt(Bundle bundle) {
		return bundle.adapt(Object.class);
	}
}
