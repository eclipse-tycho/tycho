/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.packaging;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class AttachedZipTest extends AbstractTychoIntegrationTest {

    @Test
    public void testAttachP2Metadata() throws Exception {
        Verifier verifier = getVerifier("/packaging.attachedZip", false);
        verifier.executeGoal("verify");
        verifier.verifyErrorFreeLog();
    }
}
