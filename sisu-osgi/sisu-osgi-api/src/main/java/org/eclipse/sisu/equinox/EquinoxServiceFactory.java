/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox;

import java.util.function.Supplier;

/**
 * Interface to access OSGi services in an Equinox runtime.
 */
public interface EquinoxServiceFactory {

    <T> T getService(Class<T> clazz);

    <T> T getService(Class<T> clazz, String filter);

    /**
     * Creates a supplier that dynamically lookup the requested service on the call of get, ensuring
     * that the service is present
     * 
     * @param <T>
     * @param clazz
     * @return a supplier for the given service
     */
    default <T> Supplier<T> getServiceSupplier(Class<T> clazz) {
        return () -> {
            T service = getService(clazz);
            if (service == null) {
                throw new IllegalStateException("No service of type " + clazz.getName() + " is currently available!");
            }
            return null;
        };
    }

}
