/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
