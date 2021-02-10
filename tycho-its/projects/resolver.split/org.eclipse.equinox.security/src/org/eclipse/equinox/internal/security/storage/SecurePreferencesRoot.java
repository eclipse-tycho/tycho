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

import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.friends.*;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;

/**
 * Root secure preference node. In addition to usual things it stores location, modified
 * status, encryption algorithm, and performs save and load. 
 */
public class SecurePreferencesRoot extends SecurePreferences implements IStorageConstants {

	private static final String VERSION_KEY = "org.eclipse.equinox.security.preferences.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	/**
	 * Node path reserved for persisted preferences of the modules.
	 */
	static final public String PROVIDER_PATH = "org.eclipse.equinox.security.storage.impl"; //$NON-NLS-1$

	/**
	 * Description of the property file - information only
	 */
	final private static String description = "Equinox secure storage version 1.0"; //$NON-NLS-1$

	/**
	 * The node used by the secure preferences itself
	 */
	private final static String PROVIDER_NODE = "/org.eclipse.equinox.secure.storage"; //$NON-NLS-1$

	/**
	 * Node used to store password verification tokens
	 */
	private final static String PASSWORD_VERIFICATION_NODE = PROVIDER_NODE + "/verification"; //$NON-NLS-1$

	/**
	 * Text used to verify password
	 */
	private final static String PASSWORD_VERIFICATION_SAMPLE = "-> brown fox jumped over lazy dog <-"; //$NON-NLS-1$

	/**
	 * Maximum unsuccessful decryption attempts per operation
	 */
	static protected final int MAX_ATTEMPTS = 20;

	static private ILock lock = Job.getJobManager().newLock();

	private URL location;

	private long timestamp = 0;

	private boolean modified = false;

	private JavaEncryption cipher = new JavaEncryption();

	private Map<String, PasswordExt> passwordCache = new HashMap<>(5); // cached passwords: module ID -> PasswordExt 

	public SecurePreferencesRoot(URL location) throws IOException {
		super(null, null);
		this.location = location;
		load();
	}

	public URL getLocation() {
		return location;
	}

	public JavaEncryption getCipher() {
		return cipher;
	}

	@Override
	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public void load() throws IOException {
		if (location == null)
			return;

		Properties properties = new Properties();
		InputStream is = null;
		try {
			is = StorageUtils.getInputStream(location);
			if (is != null) {
				properties.load(is);
				timestamp = getLastModified();
			}
		} catch (IllegalArgumentException e) {
			String msg = NLS.bind(SecAuthMessages.badStorageURL, location.toString());
			AuthPlugin.getDefault().logError(msg, e);
			location = null; // don't attempt to use it 
			return;
		} finally {
			if (is != null)
				is.close();
		}

		// In future new versions could be added
		Object version = properties.get(VERSION_KEY);
		if ((version != null) && !VERSION_VALUE.equals(version))
			return;
		properties.remove(VERSION_KEY);

		// Process encryption algorithms
		if (properties.containsKey(CIPHER_KEY) && properties.containsKey(KEY_FACTORY_KEY)) {
			Object cipherAlgorithm = properties.get(CIPHER_KEY);
			Object keyFactoryAlgorithm = properties.get(KEY_FACTORY_KEY);
			if ((cipherAlgorithm instanceof String) && (keyFactoryAlgorithm instanceof String))
				cipher.setAlgorithms((String) cipherAlgorithm, (String) keyFactoryAlgorithm);
			properties.remove(CIPHER_KEY);
			properties.remove(KEY_FACTORY_KEY);
		}

		for (Entry<Object, Object> entry : properties.entrySet()) {
			Object externalKey = entry.getKey();
			Object value = entry.getValue();
			if (!(externalKey instanceof String))
				continue;
			if (!(value instanceof String))
				continue;
			PersistedPath storedPath = new PersistedPath((String) externalKey);
			if (storedPath.getKey() == null)
				continue;

			SecurePreferences node = node(storedPath.getPath());
			// don't use regular put() method as that would mark node as dirty
			node.internalPut(storedPath.getKey(), (String) value);
		}
	}

	@Override
	synchronized public void flush() throws IOException {
		if (location == null)
			return;
		if (!modified)
			return;

		// check if the file has been modified since the last time it was touched
		if (timestamp != 0 && (timestamp != getLastModified())) {
			IUICallbacks callback = CallbacksProvider.getDefault().getCallback();
			if (callback != null) {
				Boolean response = callback.ask(SecAuthMessages.fileModifiedMsg);
				if (response == null)
					AuthPlugin.getDefault().frameworkLogError(SecAuthMessages.fileModifiedNote, FrameworkLogEntry.WARNING, null);
				else if (!response.booleanValue())
					return; // by default go ahead with save
			}
		}

		Properties properties = new Properties();
		properties.put(VERSION_KEY, VERSION_VALUE);

		// remember encyption algorithms
		String cipherAlgorithm = cipher.getCipherAlgorithm();
		if (cipherAlgorithm != null) {
			properties.put(CIPHER_KEY, cipherAlgorithm);
			properties.put(KEY_FACTORY_KEY, cipher.getKeyFactoryAlgorithm());
		}

		// save all user properties
		flush(properties, null);

		// output
		OutputStream stream = null;
		try {
			stream = StorageUtils.getOutputStream(location);
			properties.store(stream, description);
			modified = false;
		} finally {
			if (stream != null)
				stream.close();
		}
		timestamp = getLastModified();
	}

	/**
	 * Provides password for a new entry using:
	 * 1) default password, if any
	 * 2a) if options specify usage of specific module, that module is polled to produce password
	 * 2b) otherwise, password provider with highest priority is used to produce password
	 */
	public PasswordExt getPassword(String moduleID, IPreferencesContainer container, boolean encryption) throws StorageException {
		if (encryption) { // provides password for a new entry
			PasswordExt defaultPassword = getDefaultPassword(container);
			if (defaultPassword != null)
				return defaultPassword;
			moduleID = getDefaultModuleID(container);
		} else { // provides password for previously encrypted entry using its specified password provider module
			if (moduleID == null)
				throw new StorageException(StorageException.NO_SECURE_MODULE, SecAuthMessages.invalidEntryFormat);
			if (DEFAULT_PASSWORD_ID.equals(moduleID)) { // was default password used?
				PasswordExt defaultPassword = getDefaultPassword(container);
				if (defaultPassword != null)
					return defaultPassword;
				throw new StorageException(StorageException.NO_SECURE_MODULE, SecAuthMessages.noDefaultPassword);
			}
		}
		return getModulePassword(moduleID, container);
	}

	private PasswordExt getModulePassword(String moduleID, IPreferencesContainer container) throws StorageException {
		if (DEFAULT_PASSWORD_ID.equals(moduleID)) // this should never happen but add this check just in case
			throw new StorageException(StorageException.NO_PASSWORD, SecAuthMessages.loginNoPassword);

		PasswordProviderModuleExt moduleExt = PasswordProviderSelector.getInstance().findStorageModule(moduleID);
		String key = moduleExt.getID();
		PasswordExt passwordExt = null;
		boolean validPassword = false;
		boolean setupPasswordRecovery = false;
		boolean addedNoPrompt = false;

		try {
			lock.acquire(); // make sure process of password creation is not re-entered by another thread
			// Quick check first: it is cached?
			synchronized (passwordCache) {
				if (passwordCache.containsKey(key))
					return passwordCache.get(key);
			}

			// if this is (a headless run or JUnit) and prompt hint is not set up, set it to "false"
			boolean supressPrompts = !CallbacksProvider.getDefault().runningUI() || InternalExchangeUtils.isJUnitApp();
			if (supressPrompts && container != null && !container.hasOption(IProviderHints.PROMPT_USER)) {
				((SecurePreferencesContainer) container).setOption(IProviderHints.PROMPT_USER, Boolean.FALSE);
				addedNoPrompt = true;
			}

			// is there password verification string already?
			SecurePreferences node = node(PASSWORD_VERIFICATION_NODE);
			boolean newPassword = !node.hasKey(key);
			int passwordType = newPassword ? PasswordProvider.CREATE_NEW_PASSWORD : 0;

			for (int i = 0; i < MAX_ATTEMPTS; i++) {
				PBEKeySpec password = moduleExt.getPassword(container, passwordType);
				if (password == null)
					return null;
				passwordExt = new PasswordExt(password, key);
				if (newPassword) {
					String test = createTestString();
					CryptoData encryptedValue = getCipher().encrypt(passwordExt, StorageUtils.getBytes(test));
					node.internalPut(key, encryptedValue.toString());
					markModified();
					setupPasswordRecovery = true;
					validPassword = true;
					break;
				}
				// verify password using sample text
				String encryptedData = node.internalGet(key);
				CryptoData data = new CryptoData(encryptedData);
				try {
					byte[] decryptedData = getCipher().decrypt(passwordExt, data);
					String test = StorageUtils.getString(decryptedData);
					if (verifyTestString(test)) {
						validPassword = true;
						break;
					}
				} catch (IllegalBlockSizeException | BadPaddingException e) {
					if (!moduleExt.changePassword(e, container))
						break;
				}
			}
			if (validPassword) {
				synchronized (passwordCache) {
					passwordCache.put(key, passwordExt);
				}
			}
		} finally {
			if (addedNoPrompt) {
				((SecurePreferencesContainer) container).removeOption(IProviderHints.PROMPT_USER);
				addedNoPrompt = false;
			}
			lock.release();
		}

		if (!validPassword)
			throw new StorageException(StorageException.NO_PASSWORD, SecAuthMessages.loginNoPassword);
		if (setupPasswordRecovery)
			CallbacksProvider.getDefault().setupChallengeResponse(key, container);
		return passwordExt;
	}

	/**
	 * Retrieves default password from options, if any
	 */
	private PasswordExt getDefaultPassword(IPreferencesContainer container) {
		if (container.hasOption(IProviderHints.DEFAULT_PASSWORD)) {
			Object passwordHint = container.getOption(IProviderHints.DEFAULT_PASSWORD);
			if (passwordHint instanceof PBEKeySpec)
				return new PasswordExt((PBEKeySpec) passwordHint, DEFAULT_PASSWORD_ID);
		}
		return null;
	}

	/**
	 * Retrieves requested module ID from options, if any
	 */
	private String getDefaultModuleID(IPreferencesContainer container) {
		if (container.hasOption(IProviderHints.REQUIRED_MODULE_ID)) {
			Object idHint = container.getOption(IProviderHints.REQUIRED_MODULE_ID);
			if (idHint instanceof String)
				return (String) idHint;
		}
		return null;
	}

	public boolean onChangePassword(IPreferencesContainer container, String moduleID) {
		// validation: must have a password module
		PasswordProviderModuleExt moduleExt;
		try {
			moduleExt = PasswordProviderSelector.getInstance().findStorageModule(moduleID);
		} catch (StorageException e) {
			return false; // no module -> nothing to do
		}

		// obtain new password first
		int passwordType = PasswordProvider.CREATE_NEW_PASSWORD | PasswordProvider.PASSWORD_CHANGE;
		PBEKeySpec password = moduleExt.getPassword(container, passwordType);
		if (password == null)
			return false;

		// create verification node
		String key = moduleExt.getID();
		PasswordExt passwordExt = new PasswordExt(password, key);
		CryptoData encryptedValue;
		try {
			String test = createTestString();
			encryptedValue = getCipher().encrypt(passwordExt, StorageUtils.getBytes(test));
		} catch (StorageException e) {
			String msg = NLS.bind(SecAuthMessages.encryptingError, key, PASSWORD_VERIFICATION_NODE);
			AuthPlugin.getDefault().logError(msg, e);
			return false;
		}

		SecurePreferences node = node(PASSWORD_VERIFICATION_NODE);
		node.internalPut(key, encryptedValue.toString());
		markModified();
		try {
			flush();
		} catch (IOException e) {
			String msg = NLS.bind(SecAuthMessages.encryptingError, key, PASSWORD_VERIFICATION_NODE);
			AuthPlugin.getDefault().logError(msg, e);
			return false;
		}

		// store password in the memory cache
		cachePassword(key, passwordExt);
		CallbacksProvider.getDefault().setupChallengeResponse(key, container);
		return true;
	}

	public void cachePassword(String moduleID, PasswordExt passwordExt) {
		synchronized (passwordCache) {
			passwordCache.put(moduleID, passwordExt);
		}
	}

	public void clearPasswordCache() {
		synchronized (passwordCache) {
			passwordCache.clear();
		}
	}

	private long getLastModified() {
		File file = new File(location.getPath());
		return file.lastModified();
	}

	/**
	 * Generates random string to be stored for password verification. String format:
	 * <random1>\t<random2>\t<random2>\t<random1>
	 */
	private String createTestString() {
		SecureRandom rand = new SecureRandom();
		rand.setSeed(System.currentTimeMillis());

		long num1 = rand.nextInt(10000);
		long num2 = rand.nextInt(10000);

		StringBuilder tmp = new StringBuilder();
		tmp.append(num1);
		tmp.append('\t');
		tmp.append(num2);
		tmp.append('\t');
		tmp.append(num2);
		tmp.append('\t');
		tmp.append(num1);

		return tmp.toString();
	}

	/**
	 * Checks if the string is the hard-coded original password verification sample
	 * or a string generated according to the rules in {@link #createTestString()}.  
	 */
	private boolean verifyTestString(String test) {
		if (test == null || test.length() == 0)
			return false;
		// backward compatibility: check if it is the original hard-coded string 
		if (PASSWORD_VERIFICATION_SAMPLE.equals(test))
			return true;
		String[] parts = test.split("\t"); //$NON-NLS-1$
		if (parts == null || parts.length == 0)
			return false;
		if (parts.length != 4)
			return false;
		long num1 = -1;
		long num2 = -1;
		for (int i = 0; i < 4; i++) {
			if (parts[i] == null || parts[i].length() == 0)
				return false;
			try {
				switch (i) {
					case 0 :
						num1 = Long.decode(parts[i]).longValue();
						break;
					case 1 :
						num2 = Long.decode(parts[i]).longValue();
						break;
					case 2 : {
						long tmp = Long.decode(parts[i]).longValue();
						if (tmp != num2)
							return false;
						break;
					}
					case 3 : {
						long tmp = Long.decode(parts[i]).longValue();
						if (tmp != num1)
							return false;
						break;
					}
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
}
