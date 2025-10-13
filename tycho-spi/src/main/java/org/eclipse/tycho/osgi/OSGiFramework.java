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
package org.eclipse.tycho.osgi;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Function;

import org.osgi.framework.BundleContext;

/**
 * A launched Framework that allows to control the state of the OSGi Framework
 */
public interface OSGiFramework extends AutoCloseable {

    /**
     * Runs the given action in the framework, the action will be supplied with the a bundle context
     * to perform its action and return a result. Depending on framework type it might be required
     * to serialize the code, execute it in a remote process and serialize its return type. It
     * should therefore be ensured to use minimal API and all call parameters to be properly
     * encoded.
     * 
     * @param <R>
     * @param <Action>
     * @param action
     * @return the result of the call
     * @throws IOException
     *             if executing the call fails
     */
    <Action extends Function<BundleContext, R> & Serializable, R extends Serializable> R runInFramework(Action action)
            throws IOException;

    @Override
    void close();
}
