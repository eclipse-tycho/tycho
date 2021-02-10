/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.auth.nls;

import org.eclipse.osgi.util.NLS;

public class SecAuthMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.security.auth.nls.messages"; //$NON-NLS-1$

	// General use
	public static String instantiationFailed;
	public static String instantiationFailed1;
	public static String nullArguments;
	public static String noCallbackhandlerService;
	public static String badServicelistenerSyntaxString;
	public static String serviceAlreadyInstalled;
	public static String loginadminServiceNotIntstalled;
	public static String elementUnexpected;
	public static String loginFailure;
	public static String startFirst;
	public static String stopFirst;
	public static String unsupoprtedCharEncoding;
	public static String badStorageURL;

	// Configuration provider
	public static String unexpectedConfigElement;
	public static String invalidConfigURL;
	public static String badProviderUrl;
	public static String providerUrlUnspecified;

	// Configuration aggregator
	public static String nonExistantJaasConfig;
	public static String duplicateJaasConfig1;
	public static String duplicateJaasConfig2;

	// Configuration loaders
	public static String invalidControlFlag;
	public static String configurationEntryInvalid;
	public static String configurationEntryTypeUnknown;
	public static String documentSystemIdInvalid;
	public static String configurationEntryDuplicate;
	public static String invalidDocument;
	public static String documentExceptionIO;
	public static String documentExceptionParsing;
	public static String configurationDuplicate;

	// LoginModule loader
	public static String invalidLoginmoduleCount;

	// LoginModule proxy
	public static String loginmoduleFactoryNotSet;
	public static String loginmoduleFactoryAlreadySet;
	public static String unsetLoginmoduleFactoryError;

	// Secure storage
	public static String loginFileURL;
	public static String loginNoPassword;
	public static String loginNoDefaultLocation;
	public static String handleIncorrectFormat;
	public static String noDigestAlgorithm;
	public static String noSecureStorageModule;
	public static String noSecureStorageModules;
	public static String entryTypeIsNull;
	public static String entryTypeInvalid;
	public static String qualifierInvalid;
	public static String qualifierIsNull;
	public static String removedNode;
	public static String invalidNodePath;
	public static String errorOnSave;
	public static String keyringNotAvailable;
	public static String noDefaultPassword;
	public static String invalidEntryFormat;
	public static String noAlgorithms;
	public static String noAlgorithm;
	public static String usingAlgorithm;
	public static String decryptingError;
	public static String encryptingError;
	public static String noDigest;
	public static String failedCreateRecovery;
	public static String initCancelled;
	public static String unableToReadPswdFile;
	public static String fileModifiedMsg;
	public static String fileModifiedNote;
	public static String storedClearText;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, SecAuthMessages.class);
	}
}