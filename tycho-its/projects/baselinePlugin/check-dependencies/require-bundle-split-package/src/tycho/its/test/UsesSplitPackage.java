package tycho.its.test;

import java.net.URI;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.URIUtil;

/**
 * Uses types from the split package org.eclipse.core.runtime contributed by
 * two different bundles:
 * <ul>
 * <li>URIUtil.append(URI, String) from org.eclipse.equinox.common</li>
 * <li>IConfigurationElement.createExecutableExtension(String) from
 * org.eclipse.equinox.registry</li>
 * </ul>
 * The checker must not attribute registry types to common or vice versa.
 */
public class UsesSplitPackage {
	public URI appendUri() throws Exception {
		return URIUtil.append(new URI("http://example.com"), "path");
	}

	public Object createExtension(IConfigurationElement element) throws Exception {
		return element.createExecutableExtension("class");
	}
}
