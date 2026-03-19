package tycho.its.test;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Uses SWT Display and Shell from the org.eclipse.swt bundle. The host bundle
 * org.eclipse.swt has Eclipse-ExtensibleAPI: true and its JAR is empty — real
 * classes live in platform-specific fragments (e.g.,
 * org.eclipse.swt.gtk.linux.x86_64). The checker must resolve the fragment to
 * find the actual classes and not report false positives.
 */
public class UsesSwtDirectly {
	public Shell createShell() {
		Display display = Display.getDefault();
		return new Shell(display);
	}
}
