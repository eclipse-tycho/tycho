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


/**
 * Interface to access OSGi services in an Equinox runtime.
 */
public interface OSGiServiceFactory {

    public <T> T getService(Class<T> clazz);

    public <T> T getService(Class<T> clazz, String filter);

}
