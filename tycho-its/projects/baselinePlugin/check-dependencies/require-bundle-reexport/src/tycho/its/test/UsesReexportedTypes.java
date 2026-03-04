package tycho.its.test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Uses CoreException from org.eclipse.core.runtime package. CoreException is
 * actually provided by org.eclipse.equinox.common which is re-exported by
 * org.eclipse.core.runtime via visibility:=reexport. The checker must not
 * blame org.eclipse.core.runtime for missing CoreException types because they
 * come from a re-exported bundle.
 */
public class UsesReexportedTypes {
	public IStatus handleException(CoreException e) {
		return e.getStatus();
	}
}
