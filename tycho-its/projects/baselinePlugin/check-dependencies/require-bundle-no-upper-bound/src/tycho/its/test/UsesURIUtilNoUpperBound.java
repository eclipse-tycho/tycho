package tycho.its.test;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;

/**
 * Uses URIUtil.append(URI, String) from org.eclipse.equinox.common.
 * The MANIFEST declares Require-Bundle with simple version "3.3.0" (no upper bound).
 * Expected: range should become [X.Y.Z,4) instead of [X.Y.Z.qualifier,null).
 */
public class UsesURIUtilNoUpperBound {
	public URI test() throws Exception {
		return URIUtil.append(new URI("http://example.com"), "path");
	}
}
