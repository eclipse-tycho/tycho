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
package org.eclipse.equinox.security.storage;

import java.io.IOException;

/**
 * This interface describes functionality provided by secure preferences. Secure
 * preferences can be used to persist sensitive information in an encrypted form.
 * <p>
 * Logically, secure preferences combine functionality of a keyring (keystore) and 
 * {@link org.osgi.service.prefs.Preferences}.
 * </p><p>
 * For an excellent detailed description of the preferences functionality see
 * {@link org.osgi.service.prefs.Preferences}. To recap in a short form, preferences 
 * provide a tree. Nodes on that tree can be used to specify context. For instance, 
 * root node could have a child node called "cvs" to store information related to CVS 
 * integration.
 * </p><p>
 * Each node can have a map of keys with associated values. For instance, to store 
 * password for the CVS repository located on eclipse.org we could use the following 
 * code:
 * </p>
 * <pre>
 * 		ISecurePreferences root = SecurePreferencesFactory.getDefault();
 * 		ISecurePreferences node = root.node("cvs/eclipse.org");
 * 		node.put("password", myPassword, true);
 * </pre>
 * <p>
 * This interface has the following differences from the {@link org.osgi.service.prefs.Preferences}:</p>
 * <ul>
 * <li>get...() and put...() methods throw StorageException</li>
 * <li>put...() methods have an extra argument bEncrypt</li>
 * <li>Methods that used to throw BackingStoreException will be throwing more detailed StorageException</li> 
 * <li>Navigation on preferences tree will return ISecurePreferences, as opposing to Preferences</li>
 * <li>flush() throws IOException</li>
 * <li>sync() method is removed</li>
 * </ul> 
 * <p>
 * On the keyring side, when adding a key to the node, you can ask framework to encrypt it. Framework
 * will lazily acquire password from password provider and use it to encrypt all new  entries added 
 * to the secure preferences tree in this session. 
 * </p><p>
 * It is worthwhile to reiterate that same limitations as {@link org.osgi.service.prefs.Preferences}
 * apply to the node names. One non-trivial limitation is that node names can not contain forward 
 * slashes. For convenience, utility methods {@link EncodingUtils#encodeSlashes(String)} and
 * {@link EncodingUtils#decodeSlashes(String)} are provided to work around this limitation.
 * </p><p>
 * Also note that secure preferences only intended to store relatively small size data, such as 
 * passwords. If you need to securely store large objects, consider encrypting such objects in 
 * a symmetric way using randomly generated password and use secure preferences to store the password. 
 * </p><p>
 * If secure preferences were modified, the framework will automatically save them on shutdown.
 * </p><p>
 * This interface is not intended to be implemented or extended by clients.
 * </p>
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISecurePreferences {

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void put(String key, String value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public String get(String key, String def) throws StorageException;

	/**
	 * Removes value associated with the key.
	 * @param key key with which a value is associated
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public void remove(String key);

	/**
	 * Removes all values from this node.
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public void clear();

	/**
	 * Returns keys that have associated values.
	 * @return keys that have associated values
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public String[] keys();

	/**
	 * Returns names of children nodes
	 * @return names of children nodes
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public String[] childrenNames();

	/**
	 * Returns parent of this node
	 * @return parent of this node
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public ISecurePreferences parent();

	/**
	 * Returns node corresponding to the path specified. If such node does not
	 * exist, a new node is created.
	 * <p>
	 * If the node path is invalid, an {@link IllegalArgumentException} will be thrown
	 * by this method. The valid node path:
	 * </p>
	 * <ul>
	 * <li>contains only ASCII characters between 32 and 126 (ASCII alphanumeric and printable
	 * characters);</li>
	 * <li>can not contain two or more consecutive forward slashes;</li>
	 * <li>can not end with a trailing forward slash.</li>
	 * </ul>
	 * @see org.osgi.service.prefs.Preferences
	 * @see org.osgi.service.prefs.Preferences#node(String)
	 * @param pathName absolute or relative path to the node
	 * @return node corresponding to the path
	 * @throws IllegalArgumentException if the path name is invalid.
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public ISecurePreferences node(String pathName);

	/**
	 * Checks if the node corresponding to the specified path exists in this tree
	 * of secure preferences.
	 * <p>
	 * If the node path is invalid, an {@link IllegalArgumentException} will be thrown
	 * by this method. See {@link #node(String)} for the description of what is considered
	 * to be a valid path.
	 * </p>
	 * @see org.osgi.service.prefs.Preferences
	 * @see org.osgi.service.prefs.Preferences#node(String)
	 * @param pathName absolute or relative path to the node
	 * @return <code>true</code> if node corresponding to the path exists, <code>false</code> otherwise 
	 * @throws IllegalArgumentException if the path name is invalid.
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public boolean nodeExists(String pathName);

	/**
	 * Removes this node from the tree of secure preferences.
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public void removeNode();

	/**
	 * Returns name of this node.
	 * @return name of this node
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public String name();

	/**
	 * Returns absolute path to this node.
	 * @return absolute path to this node
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public String absolutePath();

	/**
	 * Saves the tree of secure preferences to the persistent storage. This method can be called
	 * on any node in the secure preference tree.
	 * @throws IOException if error occurred while saving secure preferences
	 */
	public void flush() throws IOException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putInt(String key, int value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public int getInt(String key, int def) throws StorageException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putLong(String key, long value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public long getLong(String key, long def) throws StorageException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putBoolean(String key, boolean value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public boolean getBoolean(String key, boolean def) throws StorageException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putFloat(String key, float value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public float getFloat(String key, float def) throws StorageException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putDouble(String key, double value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public double getDouble(String key, double def) throws StorageException;

	/**
	 * Stores a value associated with the key in this node. 
	 * @param key key with which the value is going to be associated
	 * @param value value to store
	 * @param encrypt <code>true</code> if value is to be encrypted, <code>false</code> value 
	 * does not need to be encrypted 
	 * @throws StorageException if exception occurred during encryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 * @throws NullPointerException if <code>key</code> is <code>null</code>.
	 */
	public void putByteArray(String key, byte[] value, boolean encrypt) throws StorageException;

	/**
	 * Retrieves a value associated with the key in this node. If the value was encrypted,
	 * it is decrypted.
	 * @param key key with this the value is associated
	 * @param def default value to return if the key is not associated with any value
	 * @return value associated the key. If value was stored in an encrypted form, it will be decrypted
	 * @throws StorageException if exception occurred during decryption
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public byte[] getByteArray(String key, byte[] def) throws StorageException;

	/**
	 * Specifies if value associated with the key is encrypted.
	 * @param key key with which a value is associated
	 * @return <code>true</code> if value is encrypted, <code>false</code> otherwise
	 * @throws StorageException if stored data is invalid
	 * @throws IllegalStateException if this node (or an ancestor) has been removed with 
	 * the {@link #removeNode()} method.
	 */
	public boolean isEncrypted(String key) throws StorageException;
}
