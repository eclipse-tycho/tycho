/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package bundle.test;

import junit.framework.TestCase;
import bundle.UTF8Source;

public class UTF8SourceTest extends TestCase {

	public void test() {
		UTF8Source utf8 = new UTF8Source();

		assertEquals("\u041F\u043E-\u0440\u0443\u0441\u0441\u043A\u0438", utf8.getText());
	}

}
