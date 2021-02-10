/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

import java.net.URL;
import javax.security.auth.callback.CallbackHandler;
import org.eclipse.equinox.internal.security.auth.SecureContext;

/**
 * The LoginContextFactory class is the entry point for the login support for the platform.
 * Use it to create login contexts.
 * <p>
 * This class is not intended to be instantiated or extended by clients.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class LoginContextFactory {

	/**
	 * Creates application-specific security context. The security context then can be used
	 * to perform login, logout, and obtain Subject information.
	 * <p>
	 * Due to the way default Java Configuration is initialized, this context should be
	 * created first. If standard JAAS files are used with the standard configuration,
	 * the initialization will fail unless this context created first, prior to
	 * any calls to {@link #createContext(String)}.
	 * </p>
	 * @param configName the name of login configuration to use
	 * @param configFile points to the standard JAAS configuration file 
	 * @return new security context
	 */
	public static ILoginContext createContext(String configName, URL configFile) {
		return new SecureContext(configName, configFile, null);
	}

	/**
	 * Creates application-specific security context. The security context then can be used
	 * to perform login, logout, and obtain Subject information.
	 * <p>
	 * Due to the way default Java Configuration is initialized, this context should be
	 * created first. If standard JAAS files are used with the standard configuration,
	 * the initialization will fail unless this context created first, prior to
	 * any calls to {@link #createContext(String)}.
	 * </p>
	 * @param configName the name of login configuration to use
	 * @param configFile points to the standard JAAS configuration file 
	 * @param handler optional callback handler, might be <code>null</code>
	 * @return new security context
	 */
	public static ILoginContext createContext(String configName, URL configFile, CallbackHandler handler) {
		return new SecureContext(configName, configFile, handler);
	}

	/**
	 * Creates application-specific security context. The security context then can be used
	 * to perform login, logout, and obtain Subject information.
	 * @param configName the name of login configuration to use
	 * @return new security context
	 */
	public static ILoginContext createContext(String configName) {
		return new SecureContext(configName);
	}

}
