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
package org.eclipse.sisu.equinox.embedder;

import java.util.Dictionary;

import org.eclipse.sisu.equinox.EquinoxServiceFactory;

/**
 * Interface to configure an embedded Equinox runtime. Implement an {@link EquinoxLifecycleListener}
 * component to be notified about instances of this type.
 */
public interface EmbeddedEquinox {

    EquinoxServiceFactory getServiceFactory();

    public <T> void registerService(Class<T> clazz, T service);

    public <T> void registerService(Class<T> clazz, T service, Dictionary<String, ?> properties);

}
