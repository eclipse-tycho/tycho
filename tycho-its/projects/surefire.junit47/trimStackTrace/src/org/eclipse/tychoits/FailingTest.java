/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.tychoits;

import org.junit.Assert;

import org.junit.Test;

public class FirstTest {

	@Test
	public void firstTest() throws Exception {
		this.stackMethod("FAIL!");
	}
	
	public void stackMethod(String message) {
	    Assert.fail(message);
	}
}
