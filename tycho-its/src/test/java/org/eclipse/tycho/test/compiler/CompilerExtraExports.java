/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.compiler;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CompilerExtraExports extends AbstractTychoIntegrationTest {

    @Test
    public void testExtraExports() throws Exception {
        Verifier verifier = getVerifier("compiler.extraExports", false);
        verifier.executeGoal("compile");
    }

}
