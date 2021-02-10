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
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;

/**
 * Sometimes it might be desirable to pass some context information to the password 
 * provider modules (such as a need to run without UI prompts). Below are some pre-defined
 * options that can be used to exchange information between creators of secure storage 
 * and password providers.
 * <p>
 * Options can be specified as an entry in the options map 
 * {@link SecurePreferencesFactory#open(java.net.URL, java.util.Map)}. 
 * </p><p>
 * Password provider modules are advised to take into consideration those options when 
 * applicable; note, however, that it is up to specific module to decide if (and how) 
 * they would respond to an option.
 * </p><p>
 * The set of options is open-ended and not limited to options specified below; modules can 
 * choose to process additional hints.
 * </p><p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IProviderHints {

	/**
	 * Specifies the required password provider module to be used with the storage. Expected value: {@link String}.
	 */
	static final public String REQUIRED_MODULE_ID = "org.eclipse.equinox.security.storage.requiredID"; //$NON-NLS-1$

	/**
	 * Specifies if it is possible to prompt user. Expected value: {@link Boolean}.
	 */
	static final public String PROMPT_USER = "org.eclipse.equinox.security.storage.promptUser"; //$NON-NLS-1$

	/**
	 * Storage will use this password. Expected value: {@link PBEKeySpec}.
	 */
	static final public String DEFAULT_PASSWORD = "org.eclipse.equinox.security.storage.defaultPassword"; //$NON-NLS-1$
}
