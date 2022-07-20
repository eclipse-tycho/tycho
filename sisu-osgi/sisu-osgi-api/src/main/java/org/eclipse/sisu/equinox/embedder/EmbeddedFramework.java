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
package org.eclipse.sisu.equinox.embedder;

import java.util.Dictionary;

import org.eclipse.sisu.equinox.OSGiServiceFactory;

/**
 * Interface to configure an embedded Equinox runtime. Implement an {@link FrameworkLifecycleListener}
 * component to be notified about instances of this type.
 */
public interface EmbeddedFramework {

    OSGiServiceFactory getServiceFactory();

    public <T> void registerService(Class<T> clazz, T service);

    public <T> void registerService(Class<T> clazz, T service, Dictionary<String, ?> properties);

}
