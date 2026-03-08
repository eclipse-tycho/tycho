package tycho.its.test;

import org.eclipse.swt.graphics.Point;

/**
 * Uses Point.clone() which was added as a public method in SWT 3.132 (covariant
 * return type override of Object.clone()). With a lower bound of 3.131.0, the
 * checker must detect that SWT 3.131.0 is missing this method and bump the
 * lower bound to 3.132.0, proving that fragment class discovery works for
 * detecting actual API changes.
 */
public class UsesSwtBump {
	public Point clonePoint(Point p) {
		return p.clone();
	}
}
