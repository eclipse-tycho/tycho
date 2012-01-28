/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/

package test.optionalDependencies;

import org.eclipse.core.runtime.Platform;

import junit.framework.TestCase;

public class OptionalDependenciesInTestRuntimeTest extends TestCase {
    public void testOptionalDependenciesAreNotInTestRuntime() throws Exception {
        // this test should run without the optional dependencies of our bundle under test
        assertNull(Platform.getBundle("org.eclipse.equinox.frameworkadmin"));
    }
}
