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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package compiler.additional.bundles;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.core.IsAnything;
import org.junit.Test;

public class CompileMe {

	
	@Test
	public void junit() {
		assertThat("this should compile", IsAnything.anything());
	}
}
