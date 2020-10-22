/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package bundle.tests;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import bundle.src.Src;
import bundle.src2.Src2;
import bundle.src3.Src3;

public class MultipleJarsTest extends TestCase {

	public void testSrc() {
		Bundle bundle = Platform.getBundle("bundle");
		
//		fail("Need a good way to check if classes are coming from nested JARs");
		//TODO compilation means success??

		String name = getResourceName(Src.class);
//		assertNotNull(bundle.getResource(name));
//		assertNotNull(bundle.getEntry(name));
//
		String name2 = getResourceName(Src2.class);
//		assertNotNull(bundle.getResource(name2));
//		assertNull(bundle.getEntry(name2));
//
		String name3 = getResourceName(Src3.class);
//		assertNotNull(bundle.getResource(name3));
//		assertNull(bundle.getEntry(name3));
	}

	private String getResourceName(Class clazz) {
		return clazz.getName().replace('.', '/') + ".class";
	}
}
