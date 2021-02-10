/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage.friends;

/**
 * Classes that wish to be notified when secure storage is deleted need to implement
 * this interface and register with InternalExchangeUtils.
 */
public interface IDeleteListener {

	/**
	 * Method will be called after secure storage is deleted.
	 */
	public void onDeleted();
}
