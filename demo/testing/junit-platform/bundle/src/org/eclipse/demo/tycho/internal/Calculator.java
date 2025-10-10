/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.demo.tycho.internal;

import org.eclipse.demo.tycho.CalculatorService;
import org.osgi.service.component.annotations.Component;

@Component
public class Calculator implements CalculatorService {

	@Override
	public int addTwoPositiveNumbers(int a, int b) {
		if (a < 0 && b < 0) {
			throw new IllegalArgumentException("neither value a nor value b is a positive number");
		}
		if (a < 0) {
			throw new IllegalArgumentException("value a is not a positive number");
		}
		if (b < 0) {
			throw new IllegalArgumentException("value b is not a positive number");
		}
		return a + b;
	}
}
