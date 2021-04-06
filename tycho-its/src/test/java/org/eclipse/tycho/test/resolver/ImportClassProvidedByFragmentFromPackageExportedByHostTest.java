/*******************************************************************************
 * Copyright (c) 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.resolver;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil.P2Repositories;
import org.junit.Test;

// Bug 572518 - Tycho 2.3.0 cannot import class exported from other bundle's fragment (p2.inf + Eclipse-ExtensibleAPI: true)
public class ImportClassProvidedByFragmentFromPackageExportedByHostTest extends AbstractTychoIntegrationTest {

    @Test
    public void testImportClassProvidedByFragmentFromPackageExportedByHost() throws Exception {
        Verifier verifier = getVerifier("/resolver.fragments/import-class-provided-by-fragment", false);
        verifier.executeGoals(Arrays.asList("clean", "compile"));
        verifier.verifyErrorFreeLog();
    }

}
