/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.equinox.embedder;

import org.eclipse.tycho.equinox.EquinoxServiceFactory;

public interface EquinoxEmbedder {
    /**
     * {@link EquinoxServiceFactory#getService(Class)} is the preferred client API to locate Equinox
     * services.
     */
    public <T> T getService(Class<T> clazz);

    /**
     * {@link EquinoxServiceFactory#getService(Class, String)} is the preferred client API to locate
     * Equinox services.
     */
    public <T> T getService(Class<T> clazz, String filter);

}
