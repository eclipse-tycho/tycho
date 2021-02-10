/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.equinox.security.auth.credentials;

import java.security.Principal;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.Subject;
import org.eclipse.equinox.internal.security.credentials.EquinoxPrivateCredential;
import org.eclipse.equinox.internal.security.credentials.EquinoxPublicCredential;

/**
 * This factory can be used by login modules to create Equinox public and private 
 * credentials. It is expected that as a result of successful login credentials
 * are added to the {@link Subject}.  
 * <p>  
 * This class is not intended to be instantiated or extended by clients.
 * </p> 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class CredentialsFactory {

	/**
	 * Login modules can use this method to create new public credentials as a result 
	 * of the login process. 
	 * @see Subject#getPublicCredentials() 
	 * @param name user's name
	 * @param primaryRole user's primary role, <code>null</code> if not available
	 * @param providerID the ID of the creator of this public credential; if provider was
	 * described as an extension, use the extension ID
	 * @return new public credential
	 */
	static public IPublicCredential publicCredential(String name, Principal primaryRole, String providerID) {
		return new EquinoxPublicCredential(name, primaryRole, providerID);
	}

	/**
	 * Login modules can use this method to create new public credentials as a result 
	 * of the login process. 
	 * @see Subject#getPublicCredentials() 
	 * @param name user's name
	 * @param roles user's roles, <code>null</code> if not available
	 * @param providerID the ID of the creator of this public credential; if provider was
	 * described as an extension, use the extension ID
	 * @return new public credential
	 */
	static public IPublicCredential publicCredential(String name, Principal[] roles, String providerID) {
		return new EquinoxPublicCredential(name, roles, providerID);
	}

	/**
	 * Login modules can use this method to create new private credentials.
	 * @see Subject#getPrivateCredentials() 
	 * @param privateKey the private key to be stored in this credential
	 * @param providerID the ID of the creator of this private credential; if provider was
	 * described as an extension, use the extension ID
	 * @return new private credential
	 */
	static public IPrivateCredential privateCredential(PBEKeySpec privateKey, String providerID) {
		return new EquinoxPrivateCredential(privateKey, providerID);
	}

}
