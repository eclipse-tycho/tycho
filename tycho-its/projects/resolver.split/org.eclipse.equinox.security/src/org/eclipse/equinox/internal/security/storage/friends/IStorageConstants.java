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
 * Constants and default values used by the secure storage.
 */
public interface IStorageConstants {

	/**
	 * Preference describing the default cipher algorithm for secure storage
	 */
	public String CIPHER_KEY = "org.eclipse.equinox.security.preferences.cipher"; //$NON-NLS-1$

	/**
	 * Preference describing the default key factory algorithm for secure storage
	 */
	public String KEY_FACTORY_KEY = "org.eclipse.equinox.security.preferences.keyFactory"; //$NON-NLS-1$

	/**
	 * Default cipher algorithm to use in secure storage
	 */
	public String DEFAULT_CIPHER = "PBEWithMD5AndDES"; //$NON-NLS-1$

	/**
	 * Default key factory algorithm to use in secure storage
	 */
	public String DEFAULT_KEY_FACTORY = "PBEWithMD5AndDES"; //$NON-NLS-1$

	/**
	 * Preference contains list of disabled password provider modules
	 */
	public String DISABLED_PROVIDERS_KEY = "org.eclipse.equinox.security.preferences.disabledProviders"; //$NON-NLS-1$
}
