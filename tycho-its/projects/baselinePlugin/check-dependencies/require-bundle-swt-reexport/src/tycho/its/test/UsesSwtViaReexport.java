package tycho.its.test;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Uses SWT Display and Shell through org.eclipse.jface which re-exports
 * org.eclipse.swt via visibility:=reexport. The checker must resolve the SWT
 * fragment (since org.eclipse.swt is an empty host bundle) through the
 * re-export chain and not report false positives about missing SWT methods.
 */
public class UsesSwtViaReexport {
	public Shell createShell() {
		Display display = Display.getDefault();
		return new Shell(display);
	}
}
