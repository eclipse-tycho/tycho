/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.equinox.security.auth;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * This is a common interface that tags a class that can be registered 
 * as a listener for security events. 
 * <p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 * @see ILoginContextListener
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILoginContextListener {

	/**
	 * This method is called before login starts.
	 * @param subject the subject being authenticated, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 */
	void onLoginStart(Subject subject);

	/**
	 * This method is called after login sequence is finished. If login
	 * exception is not null, the login failed.
	 * @param subject the subject being authenticated, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 * @param loginException <code>null</code> if login succeeded, otherwise contains
	 * exception caused login to fail 
	 */
	void onLoginFinish(Subject subject, LoginException loginException);

	/**
	 * This method is called before logout starts.
	 * @param subject the authenticated subject, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 */
	void onLogoutStart(Subject subject);

	/**
	 * This method is called after logout sequence finishes. If logout
	 * exception is not null, the logout failed.
	 * @param subject the authenticated subject, might be <code>null</code>
	 * if there is no subject associated the context at this time
	 * @param logoutException <code>null</code> if logout succeeded, otherwise contains
	 * exception caused logout to fail 
	 */
	void onLogoutFinish(Subject subject, LoginException logoutException);
}
