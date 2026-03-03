package tycho.its.test;

import org.osgi.framework.Bundle;

/**
 * Uses Bundle.adapt(Class) which was added in org.osgi.framework 1.6.0.
 * The MANIFEST declares no version for org.osgi.framework.
 * Expected: a new range like [1.6.0,2) should be generated.
 */
public class UsesAdaptNoVersion {
	public Object adapt(Bundle bundle) {
		return bundle.adapt(Object.class);
	}
}
