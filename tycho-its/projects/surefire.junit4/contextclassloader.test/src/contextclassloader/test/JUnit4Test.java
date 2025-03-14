/*******************************************************************************
 * Copyright (c) 2025 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package bundle.test;

import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class JUnit4Test {

    @Test
    public void testWithJUnit4() {
    	URL resourceOnClasspath = Thread.currentThread().getContextClassLoader().getResource("org/eclipse/core/runtime/Platform.class");
		Assert.assertNotNull(resourceOnClasspath);
    }
}
