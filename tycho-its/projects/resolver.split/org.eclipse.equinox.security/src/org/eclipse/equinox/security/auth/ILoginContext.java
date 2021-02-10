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
package org.eclipse.equinox.security.auth;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * The ILoginContext is the central entry point for the authentication support.
 * Use it to perform login, logout, and retrieve information associated with the security
 * subject.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILoginContext {

	/**
	 * Call this method to perform a login. 
	 * @see LoginContext#login()
	 * @throws LoginException if the authentication fails.
	 */
	public void login() throws LoginException;

	/**
	 * Call this method to perform a logout.
	 * @see LoginContext#logout()
	 * @throws LoginException if the logout fails.
	 */
	public void logout() throws LoginException;

	/**
	 * Retrieves the current Subject. Calling this method will force a login to occur 
	 * if the user is not already logged in.
	 * @see LoginContext#getSubject()
	 * @return the Subject
	 */
	public Subject getSubject() throws LoginException;

	/**
	 * Adds listener to be notified on security-related events.
	 * @param listener the listener to be registered
	 * @see ILoginContextListener
	 */
	public void registerListener(ILoginContextListener listener);

	/**
	 * Removes listener previously registered to receive notifications
	 * on security-related events.
	 * @param listener the listener to be unregistered
	 * @see ILoginContextListener
	 */
	public void unregisterListener(ILoginContextListener listener);

	/**
	 * The method exposes underlying JAAS LoginContext.
	 * <p>
	 * Using the LoginContext directly will bypass some of the processing offered
	 * by this interface and should be used only when other methods are not sufficient.
	 * </p>
	 * @return the underlying JAAS LoginContext
	 * @throws LoginException if exception was encountered while creating LoginContext
	 */
	public LoginContext getLoginContext() throws LoginException;

}
