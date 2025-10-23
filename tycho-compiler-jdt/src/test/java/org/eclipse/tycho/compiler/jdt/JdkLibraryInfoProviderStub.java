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

import org.codehaus.plexus.logging.Logger;

public class JdkLibraryInfoProviderStub extends DefaultJdkLibraryInfoProvider {

    private File libDetectorJar;
    private Logger logger;

    public JdkLibraryInfoProviderStub(File libDetectorJar, Logger logger) {
        this.libDetectorJar = libDetectorJar;
        this.logger = logger;
    }

    @Override
    protected File getLibDetectorJar() {
        return libDetectorJar;
    }

    @Override
    protected Logger getLog() {
        return this.logger;
    }
}
