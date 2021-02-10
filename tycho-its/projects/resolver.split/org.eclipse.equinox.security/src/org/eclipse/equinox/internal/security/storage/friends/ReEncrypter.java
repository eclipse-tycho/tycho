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
package org.eclipse.equinox.internal.security.storage.friends;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.auth.nls.SecAuthMessages;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesContainer;
import org.eclipse.equinox.internal.security.storage.SecurePreferencesWrapper;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.eclipse.osgi.util.NLS;

/**
 * The class will re-encrypt the whole preferences tree (any node on the tree 
 * can be passed in as a starting point).
 */
public class ReEncrypter {

	private class TmpElement {
		private String path; // absolute node path
		private Map<String, String> values; // <String>key -> <String>value

		public TmpElement(String path, Map<String, String> values) {
			this.path = path;
			this.values = values;
		}

		public String getPath() {
			return path;
		}

		public Map<String, String> getValues() {
			return values;
		}
	}

	final private ISecurePreferences root;
	final private String moduleID;
	private boolean processedOK = true;

	private ArrayList<TmpElement> elements = new ArrayList<>(); // List<TmpElement> 

	public ReEncrypter(ISecurePreferences prefs, String moduleID) {
		this.moduleID = moduleID;
		root = prefs.node("/"); //$NON-NLS-1$
	}

	/**
	 * The method will decrypt all data that can be decrypted into a local 
	 * memory structure.
	 */
	public boolean decrypt() {
		decrypt(root);
		return processedOK;
	}

	private void decrypt(ISecurePreferences node) {
		String[] keys = node.keys();
		if (keys.length > 0) {
			Map<String, String> map = new HashMap<>(keys.length); // could be less than that
			for (String key : keys) {
				try {
					if (!node.isEncrypted(key)) {
						continue;
					}
					if (!(node instanceof SecurePreferencesWrapper))
						continue;
					String encryptionModule = ((SecurePreferencesWrapper) node).getModule(key);
					if (encryptionModule == null)
						continue;
					if (!encryptionModule.equals(moduleID))
						continue;
					map.put(key, node.get(key, null));
				} catch (StorageException e) {
					// this value will not be re-coded
					String msg = NLS.bind(SecAuthMessages.decryptingError, key, node.absolutePath());
					AuthPlugin.getDefault().logError(msg, e);
					processedOK = false;
				}
			}
			if (map.size() != 0)
				elements.add(new TmpElement(node.absolutePath(), map));
		}
		String[] childrenNames = node.childrenNames();
		for (String childrenName : childrenNames) {
			decrypt(node.node(childrenName));
		}
	}

	/**
	 * The method try to create new password. 
	 * <p>
	 * <strong>Note</strong> that after the successful completion of this method the secure storage has
	 * new verification string and previously decoded values <b>must</b> be added via encrypt() method 
	 * or they will become unavailable via conventional APIs.
	 * </p>
	 */
	public boolean switchToNewPassword() {
		return ((SecurePreferencesWrapper) root).passwordChanging(moduleID);
	}

	/**
	 * The method will encrypt all data from the memory structure created by decrypt using current 
	 * passwords and providers. The original encrypted data will be overwritten.
	 */
	public boolean encrypt() {
		boolean result = true;

		// we'll directly inject here a requirement to use the specified module to encrypt data
		SecurePreferencesContainer container = ((SecurePreferencesWrapper) root).getContainer();
		Object originalProperty = container.getOption(IProviderHints.REQUIRED_MODULE_ID);
		container.setOption(IProviderHints.REQUIRED_MODULE_ID, moduleID);
		for (TmpElement element : elements) {
			ISecurePreferences node = root.node(element.getPath());
			Map<String, String> values = element.getValues();
			for (Entry<String, String> entry : values.entrySet()) {
				String key = entry.getKey();
				try {
					node.put(key, entry.getValue(), true);
				} catch (StorageException e) {
					// this value will not be re-coded
					String msg = NLS.bind(SecAuthMessages.encryptingError, key, node.absolutePath());
					AuthPlugin.getDefault().logError(msg, e);
					result = false;
				}
			}
		}
		if (originalProperty != null)
			container.setOption(IProviderHints.REQUIRED_MODULE_ID, originalProperty);
		else
			container.removeOption(IProviderHints.REQUIRED_MODULE_ID);
		return result;
	}

}
