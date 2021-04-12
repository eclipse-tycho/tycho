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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class OSGiRunningIT {

	@Test
	public void test() {
		assertNotNull(FrameworkUtil.getBundle(getClass()));
	}
	
	@Test
	public void willFail() {
		fail("This fail is intentional");
	}

}
