/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP AG and others.
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

    public void testOptionalDependenciesOfDependenciesAreInTestRuntime() throws Exception {
        // optional dependencies of this test bundle are ignored...
        assertNull(Platform.getBundle("org.apache.ant"));

        // ... but the optional dependencies of other bundles (including reactor bundles) are unchanged -> see bug 367701
        assertNotNull(Platform.getBundle("org.eclipse.equinox.frameworkadmin"));
    }

}
