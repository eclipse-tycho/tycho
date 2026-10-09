/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package compiler.fragments2.p2inf.bundle;

import tycho.test.host.HostClass;
import tycho.test.host.HostClassInFragment;

public class TestMe {
	public static void main(String[] args) {
		//this class resides in the tycho.test.host plugin and is therefore also reachable in tycho build
		HostClass hc = new HostClass(22);
		hc.add(33);
		System.out.println(hc.getCount());

		//this class resides in the tycho.test.host.fragment plugin.
		//The host bundle declares an explicit p2.inf requirement on the fragment (like SWT does),
		//which makes the fragment a normal resolved reactor dependency and therefore reachable in tycho build.
		HostClassInFragment hcif = new HostClassInFragment(22);
		hcif.add(33);
		System.out.println(hcif.getCount());
	}
}
