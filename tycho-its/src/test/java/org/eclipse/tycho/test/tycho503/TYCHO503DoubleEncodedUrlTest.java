/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho503;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class TYCHO503DoubleEncodedUrlTest extends AbstractTychoIntegrationTest {

    @Test
    public void testEclipseRepositoryModuleWithSpacesInPath() throws Exception {
        Verifier verifier = getVerifier("/TYCHO503Path With Spaces", false);
        verifier.executeGoal("install");
        verifier.verifyErrorFreeLog();
    }

}
