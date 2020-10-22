/*******************************************************************************
 * Copyright (c) 2011, 2015 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
