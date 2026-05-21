package tycho.its.test;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;

/**
 * Uses URIUtil.append(URI, String) from org.eclipse.equinox.common.
 * The MANIFEST declares Require-Bundle with range [3.5.0,4) which already
 * has the correct lower bound. The checker should detect this is semantically
 * equal to [3.5.0,4.0.0) and leave the manifest untouched.
 */
public class UsesURIUtilCorrectRange {
	public URI test() throws Exception {
		return URIUtil.append(new URI("http://example.com"), "path");
	}
}
