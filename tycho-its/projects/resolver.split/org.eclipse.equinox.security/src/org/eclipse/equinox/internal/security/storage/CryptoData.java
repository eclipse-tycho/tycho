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
 *     Inno-Tec Innovative Technologies GmbH - Fix for Bug 388055
 *
 *******************************************************************************/
package org.eclipse.equinox.internal.security.storage;

import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.security.storage.StorageException;

public class CryptoData {

	static final private char MODULE_ID_SEPARATOR = '\t'; // must not be a valid Base64 char

	/**
	 * Separates salt from the data; this must not be a valid Base64 character.
	 */
	static private final char SALT_SEPARATOR = ',';
	static private final char IV_SEPARATOR = ';';

	final private String moduleID;
	final private byte[] salt;
	final private byte[] iv;
	final private byte[] encryptedData;

	public CryptoData(String moduleID, byte[] salt, byte[] data, byte[] iv) {
		this.moduleID = moduleID;
		this.salt = salt;
		this.encryptedData = data;
		this.iv = iv;
	}

	public String getModuleID() {
		return moduleID;
	}

	public byte[] getSalt() {
		return salt;
	}

	public byte[] getData() {
		return encryptedData;
	}

	public byte[] getIV() {
		return iv;
	}

	public CryptoData(String data) throws StorageException {
		// separate moduleID
		int pos = data.indexOf(MODULE_ID_SEPARATOR);
		String encrypted;
		if (pos == -1) { // invalid data format
			throw new StorageException(StorageException.DECRYPTION_ERROR, SecAuthMessages.invalidEntryFormat);
		} else if (pos == 0) {
			moduleID = null;
			encrypted = data.substring(1);
		} else {
			moduleID = data.substring(0, pos);
			encrypted = data.substring(pos + 1);
		}

		// separate IV
		int ivPos = encrypted.indexOf(IV_SEPARATOR);
		if (ivPos != -1) {
			iv = Base64.decode(encrypted.substring(0, ivPos));
		} else { // this data does not provide an IV
			iv = null;
		}

		// separate salt and data
		int saltPos = encrypted.indexOf(SALT_SEPARATOR);
		if (saltPos != -1) {
			salt = Base64.decode(encrypted.substring(ivPos + 1, saltPos));
			encryptedData = Base64.decode(encrypted.substring(saltPos + 1));
		} else { // this is a "null" value
			if (encrypted.length() != 0) // double check that this is not a broken entry
				throw new StorageException(StorageException.DECRYPTION_ERROR, SecAuthMessages.invalidEntryFormat);
			salt = null;
			encryptedData = null;
		}
	}

	@Override
	public String toString() {
		StringBuffer encryptedText = (moduleID == null) ? new StringBuffer() : new StringBuffer(moduleID);
		encryptedText.append(MODULE_ID_SEPARATOR);
		if (iv != null) {
			encryptedText.append(Base64.encode(iv));
		}
		if (salt != null) {
			if (iv != null) {
				encryptedText.append(IV_SEPARATOR);
			}
			encryptedText.append(Base64.encode(salt));
		}
		if (encryptedData != null) {
			encryptedText.append(SALT_SEPARATOR);
			encryptedText.append(Base64.encode(encryptedData));
		}
		return encryptedText.toString();
	}

}
