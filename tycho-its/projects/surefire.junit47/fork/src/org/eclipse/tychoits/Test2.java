/*******************************************************************************
* Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tychoits;

import junit.framework.TestCase;

public class Test2 extends TestCase {

	public void testSecond() throws Exception {
		Test1.dumpPidFile(this);
	}
}
