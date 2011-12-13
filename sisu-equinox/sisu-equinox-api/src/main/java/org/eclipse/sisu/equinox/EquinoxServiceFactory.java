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
package org.eclipse.sisu.equinox;


/**
 * Interface to access OSGi services in an Equinox runtime.
 */
public interface EquinoxServiceFactory {

    public <T> T getService(Class<T> clazz);

    public <T> T getService(Class<T> clazz, String filter);

}
