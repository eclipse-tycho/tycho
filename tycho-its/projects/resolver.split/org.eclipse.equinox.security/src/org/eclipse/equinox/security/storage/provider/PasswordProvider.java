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

import javax.crypto.spec.PBEKeySpec;

/**
 * Password provider modules should extend this class. Secure storage will
 * ask modules for passwords used to encrypt entries stored in the secure preferences.
 * <p>
 * Password provider modules can be thought of as trusted 3rd parties used
 * to provide passwords to open keyrings containing secure preferences. They can do
 * it, for instance, by asking the user to enter password, or integrating with operating
 * system login, or exchanging information with a device such as a smart card reader.
 * </p><p>
 * Use org.eclipse.equinox.security.secureStorage extension point to contribute 
 * password provider module to the secure storage system.
 * </p> 
 */
abstract public class PasswordProvider {

	/**
	 * Bit mask for the password type field of the {@link #getPassword(IPreferencesContainer, int)}
	 * method. If value at this bit set to <code>1</code>, it indicates that a new
	 * password should be created; otherwise this is a request for the password previously 
	 * used for this secure storage.
	 */
	final public static int CREATE_NEW_PASSWORD = 1 << 0;

	/**
	 * Bit mask for the password type field of the {@link #getPassword(IPreferencesContainer, int)}
	 * method. If value at this bit set to <code>1</code>, it indicates that a new password
	 * is requested as a part of the password change operation.
	 */
	final public static int PASSWORD_CHANGE = 1 << 1;

	/**
	 * This method should return the password used to encrypt entries in the secure 
	 * preferences.
	 * @param container container of the secure preferences
	 * @param passwordType the collection of bits that describes password type requested. See
	 * {@link #CREATE_NEW_PASSWORD} and {@link #PASSWORD_CHANGE}. When evaluating value of this
	 * field use bit-wise filters as additional bits might be used in future versions
	 * @return password used to encrypt entries in the secure preferences, <code>null</code>
	 * if unable to obtain password
	 */
	abstract public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType);

	/**
	 * Constructor.
	 */
	public PasswordProvider() {
		// placeholder
	}

	/**
	 * The framework might call this method if it suspects that the password is invalid
	 * (for instance, due to a failed data decryption). 
	 * @param e exception that occurred in the secure preferences processing
	 * @param container container of the secure preferences
	 * @return <code>true</code> if a different password might be provided; <code>false</code>
	 * otherwise. If in doubt, return <code>false</code>
	 */
	public boolean retryOnError(Exception e, IPreferencesContainer container) {
		return false;
	}

}
