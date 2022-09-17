/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
