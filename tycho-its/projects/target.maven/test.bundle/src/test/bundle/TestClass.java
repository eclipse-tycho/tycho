/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package test.bundle;

import org.apache.commons.lang.ArrayUtils;
import org.apache.oro.io.AwkFilenameFilter;

public class TestClass {
	public static void main(String[] args) {
		// this has no sources in the ide
		new AwkFilenameFilter("[ab]");
		// but this has sources from the target!
		ArrayUtils.toString(args);
	}
}
