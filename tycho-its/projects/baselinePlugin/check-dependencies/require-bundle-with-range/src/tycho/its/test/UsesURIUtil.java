package tycho.its.test;

import java.net.URI;
import org.eclipse.core.runtime.URIUtil;

/**
 * Uses URIUtil.append(URI, String) from org.eclipse.equinox.common.
 * This method was added after 3.3.0 (approximately 3.5.0).
 * The MANIFEST declares Require-Bundle with range [3.3.0,4.0.0).
 * Expected: lower bound should be updated to a version without qualifier.
 */
public class UsesURIUtil {
	public URI test() throws Exception {
		return URIUtil.append(new URI("http://example.com"), "path");
	}
}
