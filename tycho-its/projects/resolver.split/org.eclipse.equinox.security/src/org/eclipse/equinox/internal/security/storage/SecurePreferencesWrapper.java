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

import java.io.IOException;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * This class combines secure preferences node with specific container. See
 * container for the description of relationships.
 */
public class SecurePreferencesWrapper implements ISecurePreferences {

	final protected SecurePreferences node;

	final protected SecurePreferencesContainer container;

	public SecurePreferencesWrapper(SecurePreferences node, SecurePreferencesContainer container) {
		this.node = node;
		this.container = container;
	}

	// Testing only
	public SecurePreferencesContainer getContainer() {
		return container;
	}

	@Override
	public String absolutePath() {
		return node.absolutePath();
	}

	@Override
	public String[] childrenNames() {
		return node.childrenNames();
	}

	@Override
	public void clear() {
		node.clear();
	}

	@Override
	public void flush() throws IOException {
		node.flush();
	}

	@Override
	public String[] keys() {
		return node.keys();
	}

	@Override
	public String name() {
		return node.name();
	}

	@Override
	public void remove(String key) {
		node.remove(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SecurePreferencesWrapper))
			return false;
		SecurePreferencesWrapper other = (SecurePreferencesWrapper) obj;
		return container.equals(other.container) && node.equals(other.node);
	}

	@Override
	public int hashCode() {
		String tmp = Integer.toString(container.hashCode()) + '|' + Integer.toString(node.hashCode());
		return tmp.hashCode();
	}

	///////////////////////////////////////////////////////////////////////////
	// Navigation

	@Override
	public ISecurePreferences node(String pathName) {
		return container.wrapper(node.node(pathName));
	}

	@Override
	public ISecurePreferences parent() {
		SecurePreferences parent = node.parent();
		if (parent == null)
			return null;
		return container.wrapper(node.parent());
	}

	@Override
	public boolean nodeExists(String pathName) {
		return node.nodeExists(pathName);
	}

	@Override
	public void removeNode() {
		container.removeWrapper(node);
		node.removeNode();
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// puts() and gets()

	@Override
	public String get(String key, String def) throws StorageException {
		return node.get(key, def, container);
	}

	@Override
	public void put(String key, String value, boolean encrypt) throws StorageException {
		node.put(key, value, encrypt, container);
	}

	@Override
	public boolean getBoolean(String key, boolean def) throws StorageException {
		return node.getBoolean(key, def, container);
	}

	@Override
	public void putBoolean(String key, boolean value, boolean encrypt) throws StorageException {
		node.putBoolean(key, value, encrypt, container);
	}

	@Override
	public int getInt(String key, int def) throws StorageException {
		return node.getInt(key, def, container);
	}

	@Override
	public void putInt(String key, int value, boolean encrypt) throws StorageException {
		node.putInt(key, value, encrypt, container);
	}

	@Override
	public float getFloat(String key, float def) throws StorageException {
		return node.getFloat(key, def, container);
	}

	@Override
	public void putFloat(String key, float value, boolean encrypt) throws StorageException {
		node.putFloat(key, value, encrypt, container);
	}

	@Override
	public long getLong(String key, long def) throws StorageException {
		return node.getLong(key, def, container);
	}

	@Override
	public void putLong(String key, long value, boolean encrypt) throws StorageException {
		node.putLong(key, value, encrypt, container);
	}

	@Override
	public double getDouble(String key, double def) throws StorageException {
		return node.getDouble(key, def, container);
	}

	@Override
	public void putDouble(String key, double value, boolean encrypt) throws StorageException {
		node.putDouble(key, value, encrypt, container);
	}

	@Override
	public byte[] getByteArray(String key, byte[] def) throws StorageException {
		return node.getByteArray(key, def, container);
	}

	@Override
	public void putByteArray(String key, byte[] value, boolean encrypt) throws StorageException {
		node.putByteArray(key, value, encrypt, container);
	}

	@Override
	public boolean isEncrypted(String key) throws StorageException {
		return node.isEncrypted(key);
	}

	public String getModule(String key) {
		return node.getModule(key);
	}

	public boolean passwordChanging(String moduleID) {
		return node.passwordChanging(container, moduleID);
	}
}
