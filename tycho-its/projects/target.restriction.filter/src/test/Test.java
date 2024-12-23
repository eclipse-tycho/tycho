/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
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
package test;

import org.osgi.framework.Version;

import junit.framework.TestCase;

public class Test extends TestCase {
    
    public void test() {
        Version version = new Version((String) Activator.context.getBundle(0).getHeaders().get("Bundle-Version"));
        assertTrue(version.compareTo(new Version("3.22")) < 0);
    }
    
}
