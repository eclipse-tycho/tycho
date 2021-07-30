/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SubtractorTest {

	@Test
	public void incrementTest() {
		CountDown counter = new CountDown(10);
		counter.decrement(1);
		counter.decrement(3);
		assertEquals(6, counter.get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void decrementTest() {
		CountDown counter = new CountDown(10);
		counter.decrement(-1);
	}

	@Test(expected = IllegalStateException.class)
	public void decrementTest2() {
		CountDown counter = new CountDown(1);
		counter.decrement(5);
	}
}
