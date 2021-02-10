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
package org.eclipse.equinox.security.storage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesMapper;
import org.eclipse.equinox.security.storage.provider.IProviderHints;

/**
 * Use this class to access secure preferences. Secure preferences allow storage of
 * data in an encrypted form.  
 * <p>
 * This class is not intended to be instantiated or extended by clients.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
final public class SecurePreferencesFactory {

	/**
	 * Returns default secure preferences.
	 * <p>
	 * The framework will attempt to open secure preferences in a user-specific location. 
	 * As a result, the information stored can be shared among all programs run by the user. 
	 * The location is determined as follows:
	 * </p>
	 * <ol>
	 * <li>&quot;-equinox.keyring&quot; command line arguments</li>
	 * <li>Java's &quot;user.home&quot; environment variable. On Windows system it usually 
	 * corresponds to the %USERPROFILE% environment variable or determined as the parent of
	 * user's desktop folder. On Unix Java usually determines it from user's entry
	 * in the password file (commonly this corresponds to $HOME environment variable);</li>
	 * <li>if it fails, preferences will use configuration location of the current Eclipse 
	 * instance.</li>
	 * </ol>
	 * @return default instance of secure preferences, <code>null</code> if application
	 * was unable to create secure preferences using default location
	 */
	static public ISecurePreferences getDefault() {
		return SecurePreferencesMapper.getDefault();
	}

	/**
	 * Returns a secure properties corresponding to the URL locations supplied. If URL 
	 * is <code>null</code>, a default location is used.
	 * <p>
	 * Note that while this method accepts URLs to account for future expandability of this API,
	 * at present the method only accepts &quot;file&quot; URLs that point to a directory. 
	 * An {@link IOException} might be thrown if unsupported URL is passed to this method.
	 * </p><p>
	 * Similarly to the rest of the Equinox, URLs passed as an argument must not be encoded, 
	 * meaning that spaces should stay as spaces, not as &quot;%x20&quot;.
	 * </p>
	 * @param location URL pointing to the location of secure storage. At present only file URLs
	 * are supported. Pass <code>null</code> to use default location
	 * @param options use to pass hints to the secure preferences implementation. Pass <code>null</code>
	 * if no options are needed. See {@link IProviderHints}
	 * @return a secure preferences
	 * @throws IOException if unsupported URLs types are passed in, or if location is not accessible
	 */
	static public ISecurePreferences open(URL location, @SuppressWarnings("rawtypes") Map options) throws IOException {
		return SecurePreferencesMapper.open(location, options);
	}
}
