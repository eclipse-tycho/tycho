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

public class Counter {

	int count;

	public void increment(int x) {
		if (x < 0) {
			throw new IllegalArgumentException();
		}
		count += x;
	}

	public int get() {
		return count;
	}
}
