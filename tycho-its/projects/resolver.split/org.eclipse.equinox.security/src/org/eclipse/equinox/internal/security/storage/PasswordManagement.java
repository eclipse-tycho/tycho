/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.osgi.util.NLS;

public class PasswordManagement {

	/**
	 * Algorithm used to digest passwords
	 */
	private static final String DIGEST_ALGORITHM = "MD5"; //$NON-NLS-1$

	/**
	 * Node used to store encrypted password for the password recovery
	 */
	private final static String PASSWORD_RECOVERY_NODE = "/org.eclipse.equinox.secure.storage/recovery"; //$NON-NLS-1$

	/**
	 * Pseudo-module ID to use when encryption is done with the default password.
	 */
	protected final static String RECOVERY_PSEUDO_ID = "org.eclipse.equinox.security.recoveryModule"; //$NON-NLS-1$

	/**
	 * Key used to store encrypted password for the password recovery
	 */
	private final static String PASSWORD_RECOVERY_KEY = "org.eclipse.equinox.security.internal.recovery.password"; //$NON-NLS-1$

	/**
	 * Key used to store questions for the password recovery
	 */
	private final static String PASSWORD_RECOVERY_QUESTION = "org.eclipse.equinox.security.internal.recovery.question"; //$NON-NLS-1$

	static public void setupRecovery(String[][] challengeResponse, String moduleID, IPreferencesContainer container) {
		// encrypt user password with the mashed-up answers and store encrypted value
		SecurePreferencesRoot root = ((SecurePreferencesContainer) container).getRootData();
		SecurePreferences node = recoveryNode(root, moduleID);

		if (challengeResponse == null) {
			node.remove(PASSWORD_RECOVERY_KEY);
			for (int i = 0; i < 2; i++) {
				String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
				node.remove(key);
			}
			root.markModified();
			return;
		}
		// create password from mixing and boiling answers
		String internalPassword = mashPassword(challengeResponse[1]);

		PasswordExt internalPasswordExt = new PasswordExt(new PBEKeySpec(internalPassword.toCharArray()), RECOVERY_PSEUDO_ID);
		PasswordExt password;
		try {
			password = root.getPassword(moduleID, container, false);
		} catch (StorageException e) {
			AuthPlugin.getDefault().logError(SecAuthMessages.failedCreateRecovery, e);
			return;
		}
		try {
			byte[] data = StorageUtils.getBytes(new String(password.getPassword().getPassword()));
			CryptoData encryptedValue = root.getCipher().encrypt(internalPasswordExt, data);
			node.internalPut(PASSWORD_RECOVERY_KEY, encryptedValue.toString());
			root.markModified();
		} catch (StorageException e) {
			AuthPlugin.getDefault().logError(SecAuthMessages.failedCreateRecovery, e);
			return;
		}

		// save questions
		for (int i = 0; i < challengeResponse[0].length; i++) {
			String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
			try {
				node.put(key, challengeResponse[0][i], false, (SecurePreferencesContainer) container);
			} catch (StorageException e) {
				// not going to happen for non-encrypted values
			}
			// already marked as modified
		}
	}

	static public String[] getPasswordRecoveryQuestions(SecurePreferencesRoot root, String moduleID) {
		// retrieve stored questions
		List<String> questions = new ArrayList<>();
		SecurePreferences node = recoveryNode(root, moduleID);
		for (int i = 0;; i++) {
			String key = PASSWORD_RECOVERY_QUESTION + Integer.toString(i + 1);
			if (!node.hasKey(key))
				break;
			try {
				String question = node.get(key, null, null);
				if (question == null)
					break;
				questions.add(question);
			} catch (StorageException e) {
				// can't happen for non-encrypted values
			}
		}
		String[] result = new String[questions.size()];
		return questions.toArray(result);
	}

	static public String recoverPassword(String[] answers, SecurePreferencesRoot root, String moduleID) {
		String internalPassword = mashPassword(answers); // create recovery password from answers

		SecurePreferences node = recoveryNode(root, moduleID);
		PasswordExt internalPasswordExt = new PasswordExt(new PBEKeySpec(internalPassword.toCharArray()), RECOVERY_PSEUDO_ID);
		try {
			CryptoData encryptedData = new CryptoData(node.internalGet(PASSWORD_RECOVERY_KEY));
			byte[] data = root.getCipher().decrypt(internalPasswordExt, encryptedData);
			return StorageUtils.getString(data);
		} catch (IllegalStateException | IllegalBlockSizeException | BadPaddingException | StorageException e) {
			return null;
		}
	}

	static private SecurePreferences recoveryNode(SecurePreferences root, String moduleID) {
		return root.node(PASSWORD_RECOVERY_NODE).node(moduleID);
	}

	/**
	 * Produces password from a list of answers:
	 * - all answers are put into one string
	 * - characters from alternating ends of the string are taken to form "mashed up" recovery 
	 * password
	 * - the secure digest of the "mashed up" string is created
	 * 
	 * This procedure should improve quality of the recovery password - even if answers 
	 * are dictionary words, digested "mashed up" password should be of a reasonable good quality 
	 */
	static private String mashPassword(String[] answers) {
		// form a string composing answers
		StringBuilder tmp = new StringBuilder();
		for (String answer : answers) {
			tmp.append(answer.trim());
		}
		// mix it up
		StringBuilder mix = new StringBuilder();
		int pos = tmp.length() - 1;
		for (int i = 0; i <= pos; i++) {
			mix.append(tmp.charAt(i));
			if (i < pos)
				mix.append(tmp.charAt(pos));
			pos--;
		}
		// create digest
		String internalPassword;
		try {
			// normally use digest of what was entered
			MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
			byte[] digested = digest.digest(StorageUtils.getBytes(mix.toString()));
			internalPassword = EncodingUtils.encodeBase64(digested);
		} catch (NoSuchAlgorithmException e) {
			// just use the text as is; it is nicer to use digest but in this case no big deal
			String msg = NLS.bind(SecAuthMessages.noDigest, DIGEST_ALGORITHM);
			AuthPlugin.getDefault().logMessage(msg);
			internalPassword = mix.toString();
		}
		return internalPassword;
	}

}
