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
 *    Christoph Läubrich. - initial API and implementation
 *******************************************************************************/
package bundle.test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdderTest {

    @org.junit.jupiter.api.Test
	public void incrementTest() {
		Counter counter = new Counter();
		counter.increment(1);
		counter.increment(3);
		assertEquals(4, counter.get());
    }

    @org.junit.jupiter.api.Test
	public void decrementTest() {
		assertThrows(IllegalArgumentException.class, ()->{
			Counter counter = new Counter();
			counter.increment(-1);
		});
	}
}
