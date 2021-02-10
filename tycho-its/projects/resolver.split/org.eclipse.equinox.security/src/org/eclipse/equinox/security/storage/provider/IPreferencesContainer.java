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
package org.eclipse.equinox.security.storage.provider;

import java.net.URL;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;

/**
 * The container of the secure preferences. Objects implementing this 
 * interface are supplied to to various provider method calls to
 * describe current context. 
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>  
 * @see PasswordProvider
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPreferencesContainer {

	/**
	 * Returns location corresponding to the secure preferences.
	 * @return location corresponding to the secure preferences
	 */
	public URL getLocation();

	/**
	 * Returns secure preferences contained in this container. 
	 * @return root node of the secure preferences contained in this container 
	 */
	public ISecurePreferences getPreferences();

	/**
	 * Determines is a given option is specified for this container. 
	 * @see  SecurePreferencesFactory#open(java.net.URL, java.util.Map) 
	 * @see IProviderHints
	 * @param key key describing the option
	 * @return <code>true</code> if container has this option; <code>false</code> otehrwise
	 */
	public boolean hasOption(Object key);

	/**
	 * Returns an option specified for this container, or <code>null</code>
	 * if the option was not specified.
	 * @param key describes the option
	 * @return value of the option for this container, or <code>null</code>
	 * the option was not specified
	 */
	public Object getOption(Object key);

}
