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

import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.Map.Entry;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.friends.*;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.osgi.util.NLS;

/**
 * Note that algorithm detection skips aliases:
 *    Alg.Alias.Cipher.ABC
 * only a few aliases are useful and it will be harder to separate human-readable
 * aliases from internal ones.
 *
 */
public class JavaEncryption {

	private final static String SECRET_KEY_FACTORY = "SecretKeyFactory."; //$NON-NLS-1$
	private final static String CIPHER = "Cipher."; //$NON-NLS-1$

	private final static String sampleText = "sample text for roundtrip testing"; //$NON-NLS-1$
	private final static PasswordExt samplePassword = new PasswordExt(new PBEKeySpec("password1".toCharArray()), "abc"); //$NON-NLS-1$ //$NON-NLS-2$

	static private ILock lock = Job.getJobManager().newLock();

	static private final int SALT_ITERATIONS = 10;

	private String keyFactoryAlgorithm = null;
	private String cipherAlgorithm = null;

	private boolean initialized = false;

	private HashMap<String, String> availableCiphers;

	public JavaEncryption() {
		// placeholder
	}

	public String getKeyFactoryAlgorithm() {
		return keyFactoryAlgorithm;
	}

	public String getCipherAlgorithm() {
		return cipherAlgorithm;
	}

	public void setAlgorithms(String cipherAlgorithm, String keyFactoryAlgorithm) {
		try {
			lock.acquire(); // avoid conflict with init()
			this.cipherAlgorithm = cipherAlgorithm;
			this.keyFactoryAlgorithm = keyFactoryAlgorithm;
		} finally {
			lock.release();
		}
	}

	private void init() throws StorageException {
		if (initialized)
			return;
		initialized = true;

		try {
			lock.acquire(); // avoid multiple simultaneous initializations
			IUICallbacks callback = CallbacksProvider.getDefault().getCallback();
			if (callback == null)
				internalInitialize();
			else {
				callback.execute(() -> internalInitialize());
			}
		} finally {
			lock.release();
		}
	}

	protected void internalInitialize() throws StorageException {
		if (cipherAlgorithm != null && keyFactoryAlgorithm != null) {
			if (roundtrip(cipherAlgorithm, keyFactoryAlgorithm))
				return;
			// this is a bad situation - JVM cipher no longer available. Both log and throw an exception
			String msg = NLS.bind(SecAuthMessages.noAlgorithm, cipherAlgorithm);
			StorageException e = new StorageException(StorageException.INTERNAL_ERROR, msg);
			AuthPlugin.getDefault().logError(msg, e);
			throw e;
		}
		if (cipherAlgorithm == null || keyFactoryAlgorithm == null) {
			IEclipsePreferences eclipseNode = ConfigurationScope.INSTANCE.getNode(AuthPlugin.PI_AUTH);
			cipherAlgorithm = eclipseNode.get(IStorageConstants.CIPHER_KEY, IStorageConstants.DEFAULT_CIPHER);
			keyFactoryAlgorithm = eclipseNode.get(IStorageConstants.KEY_FACTORY_KEY, IStorageConstants.DEFAULT_KEY_FACTORY);
		}
		if (roundtrip(cipherAlgorithm, keyFactoryAlgorithm))
			return;
		String unavailableCipher = cipherAlgorithm;

		detect();
		if (availableCiphers.size() == 0)
			throw new StorageException(StorageException.INTERNAL_ERROR, SecAuthMessages.noAlgorithms);

		// use first available
		cipherAlgorithm = availableCiphers.keySet().iterator().next();
		keyFactoryAlgorithm = availableCiphers.get(cipherAlgorithm);

		String msg = NLS.bind(SecAuthMessages.usingAlgorithm, unavailableCipher, cipherAlgorithm);
		AuthPlugin.getDefault().logMessage(msg);
	}

	public CryptoData encrypt(PasswordExt passwordExt, byte[] clearText) throws StorageException {
		init();
		return internalEncrypt(passwordExt, clearText);
	}

	private CryptoData internalEncrypt(PasswordExt passwordExt, byte[] clearText) throws StorageException {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
			SecretKey key = keyFactory.generateSecret(passwordExt.getPassword());

			byte[] salt = new byte[8];
			SecureRandom random = new SecureRandom();
			random.nextBytes(salt);
			PBEParameterSpec entropy = new PBEParameterSpec(salt, SALT_ITERATIONS);

			Cipher c = Cipher.getInstance(cipherAlgorithm);
			c.init(Cipher.ENCRYPT_MODE, key, entropy);
			byte[] iv = null;

			//check if IV is required by PBE algorithm
			AlgorithmParameterSpec paramSpec;
			try {
				paramSpec = c.getParameters().getParameterSpec(PBEParameterSpec.class).getParameterSpec();
				if (paramSpec != null && paramSpec instanceof IvParameterSpec) {
					iv = c.getIV();
				}
			} catch (InvalidParameterSpecException e) {
				/*do nothing*/
			}

			byte[] result = c.doFinal(clearText);
			return new CryptoData(passwordExt.getModuleID(), salt, result, iv);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			handle(e, StorageException.ENCRYPTION_ERROR);
			return null;
		} catch (InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		}
	}

	public byte[] decrypt(PasswordExt passwordExt, CryptoData encryptedData) throws StorageException, IllegalStateException, IllegalBlockSizeException, BadPaddingException {
		init();
		return internalDecrypt(passwordExt, encryptedData);
	}

	private byte[] internalDecrypt(PasswordExt passwordExt, CryptoData encryptedData) throws StorageException, IllegalStateException, IllegalBlockSizeException, BadPaddingException {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyFactoryAlgorithm);
			SecretKey key = keyFactory.generateSecret(passwordExt.getPassword());

			IvParameterSpec ivParamSpec = null;
			if (encryptedData.getIV() != null) {
				ivParamSpec = new IvParameterSpec(encryptedData.getIV());
			}

			PBEParameterSpec entropy = null;
			if (ivParamSpec != null) {
				entropy = new PBEParameterSpec(encryptedData.getSalt(), SALT_ITERATIONS, ivParamSpec);
			} else {
				entropy = new PBEParameterSpec(encryptedData.getSalt(), SALT_ITERATIONS);
			}

			Cipher c = Cipher.getInstance(cipherAlgorithm);
			c.init(Cipher.DECRYPT_MODE, key, entropy);

			byte[] result = c.doFinal(encryptedData.getData());
			return result;
		} catch (InvalidAlgorithmParameterException | InvalidKeyException | InvalidKeySpecException | NoSuchPaddingException | NoSuchAlgorithmException e) {
			handle(e, StorageException.INTERNAL_ERROR);
			return null;
		}
	}

	private void handle(Exception e, int internalCode) throws StorageException {
		if (AuthPlugin.DEBUG_LOGIN_FRAMEWORK)
			e.printStackTrace();
		StorageException exception = new StorageException(internalCode, e);
		throw exception;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Algorithm detection

	/**
	 * Result: Map:
	 *    <String>cipher -> <String>keyFactory
	 */
	public HashMap<String, String> detect() {
		IUICallbacks callback = CallbacksProvider.getDefault().getCallback();
		if (callback == null)
			return internalDetect();

		IStorageTask task = () -> internalDetect();
		try {
			callback.execute(task);
		} catch (StorageException e) { // should not happen in this path
			AuthPlugin.getDefault().logError(e.getMessage(), e);
		}
		return availableCiphers;
	}

	public HashMap<String, String> internalDetect() {
		Set<String> ciphers = findProviders(CIPHER);
		Set<String> keyFactories = findProviders(SECRET_KEY_FACTORY);
		availableCiphers = new HashMap<>(ciphers.size());

		for (String cipher : ciphers) {
			// check if there is a key factory with the same name
			if (keyFactories.contains(cipher)) {
				if (roundtrip(cipher, cipher)) {
					availableCiphers.put(cipher, cipher);
					continue;
				}
			}
			for (String keyFactory : keyFactories) {
				if (roundtrip(cipher, keyFactory)) {
					availableCiphers.put(cipher, keyFactory);
					continue;
				}
			}
		}
		return availableCiphers;
	}

	private Set<String> findProviders(String prefix) {
		Provider[] providers = Security.getProviders();
		Set<String> algorithms = new HashSet<>();
		int prefixLength = prefix.length();
		for (Provider provider : providers) {
			for (Entry<Object, Object> entry : provider.entrySet()) {
				Object key = entry.getKey();
				if (key == null)
					continue;
				if (!(key instanceof String))
					continue;
				String value = (String) key;
				if (value.indexOf(' ') != -1) // skips properties like "[Cipher.ABC SupportedPaddings]"
					continue;
				if (value.startsWith(prefix)) {
					String keyFactory = value.substring(prefixLength);
					algorithms.add(keyFactory);
				}
			}
		}
		return algorithms;
	}

	private boolean roundtrip(String testCipher, String testKeyFactory) {
		boolean storeInitState = initialized;
		String storedCipherAlgorithm = cipherAlgorithm;
		String storedKeyAlgorithm = keyFactoryAlgorithm;
		initialized = true;
		try {
			cipherAlgorithm = testCipher;
			keyFactoryAlgorithm = testKeyFactory;
			CryptoData encrypted = internalEncrypt(samplePassword, StorageUtils.getBytes(sampleText));
			byte[] roundtripBytes = internalDecrypt(samplePassword, encrypted);
			String result = StorageUtils.getString(roundtripBytes);
			return sampleText.equals(result);
		} catch (Exception e) {
			// internal implementation throws both checked and unchecked
			// exceptions (without much documentation to go on), so have to use catch-all
			return false;
		} finally { // reset back
			cipherAlgorithm = storedCipherAlgorithm;
			keyFactoryAlgorithm = storedKeyAlgorithm;
			initialized = storeInitState;
		}
	}

}
