/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.osgi.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.tycho.osgi.OSGiFramework;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

class EmbeddedOSGiFramework implements OSGiFramework {

    private Framework framework;
    private boolean standalone;

    EmbeddedOSGiFramework(Framework framework, boolean standalone) {
        this.framework = framework;
        this.standalone = standalone;
    }

    @Override
    public <Action extends Function<BundleContext, R> & Serializable, R extends Serializable> R runInFramework(
            Action action) throws IOException {
        if (standalone) {
            //for standalone case we can just call code directly and pass the system bundle context
            return action.apply(framework.getBundleContext());
        }
        throw new IOException("not implemented yet");
    }

    @Override
    public void close() {
        try {
            framework.stop();
        } catch (BundleException e) {
        }
        try {
            framework.waitForStop(TimeUnit.SECONDS.toMillis(30));
        } catch (InterruptedException e) {
        }
    }

}
