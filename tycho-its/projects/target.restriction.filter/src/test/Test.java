/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        assertTrue(version.compareTo(new Version("3.5")) < 0);
    }
    
}
