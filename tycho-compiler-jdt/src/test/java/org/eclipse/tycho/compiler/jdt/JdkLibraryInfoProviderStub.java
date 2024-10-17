/*******************************************************************************
 * Copyright (c) 2018 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import java.io.File;

public class JdkLibraryInfoProviderStub extends JdkLibraryInfoProvider {

    private File libDetectorJar;

    public JdkLibraryInfoProviderStub(File libDetectorJar) {
        this.libDetectorJar = libDetectorJar;
    }

    @Override
    protected File getLibDetectorJar() {
        return libDetectorJar;
    }
}
