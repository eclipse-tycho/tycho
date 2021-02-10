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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;

/**
 * NOTE NOTE NOTE: this Javadoc is implementation details - this is not an API
 * but a few notes on implementation design.
 * 
 * For a URL we get one secure preference tree that contains data. The top node
 * of that tree has a "root" type, it has special properties - the location, status,
 * and knowledge as to how persist the tree. 
 * 
 * On the user side - let's say we have the following preferences open via factory:
 * 
 * UserPreferences1 (Options1, URL1)
 * UserPreferences2 (Options2, URL1)
 * UserPreferences3 (Options3, URL2)
 * UserPreferences4 (Options4, URL2)
 * 
 * When we'll have 2 actual "back end" secure preferences tree with data: 
 * 
 * [UserPreferences1] -> [Options1] + 
 *                                    \
 *                                     [secure preferences1]   <- 1 : 1 -> URL1
 *                                    /
 * [UserPreferences2] -> [Options2] + 
 * 
 * [UserPreferences3] -> [Options3] + 
 *                                    \
 *                                     [secure preferences2]   <- 1 : 1 -> URL2
 *                                    /
 * [UserPreferences4] -> [Options4] +
 *  
 * The user-facing nodes are actually a (node + options for this container). User-facing 
 * nodes are called wrappers as they primarily wrap secure preferences nodes. 
 * 
 * Containers are used to combine all wrappers created for the set of options. This way
 * users don't have to specify options on each get...() / put...() method. 
 * 
 * Additionally, containers cache wrappers so that navigation on preferences tree won't
 * create new wrappers every time process navigates from one node on the tree to another.
 * 
 * Password provider modules:
 * 
 * Note that only a single instance of each password provider is ever created. However,
 * those instances are passed options as arguments.
 */
public class SecurePreferencesContainer implements IPreferencesContainer {

	private Map<SecurePreferences, SecurePreferencesWrapper> wrappers = new HashMap<>(); // node -> SecurePreferencesWrapper

	final private Map<Object, Object> options;
	final private SecurePreferencesRoot root;

	public SecurePreferencesContainer(SecurePreferencesRoot root, Map<Object, Object> options) {
		this.root = root;
		if (options != null) { // make a copy to avoid problems if original is modified later
			this.options = new HashMap<>(options.size());
			this.options.putAll(options);
		} else
			this.options = new HashMap<>(2);
	}

	public ISecurePreferences wrapper(SecurePreferences node) {
		synchronized (wrappers) {
			if (wrappers.containsKey(node))
				return wrappers.get(node);
			SecurePreferencesWrapper newWrapper = new SecurePreferencesWrapper(node, this);
			wrappers.put(node, newWrapper);
			return newWrapper;
		}
	}

	public void removeWrapper(SecurePreferences node) {
		synchronized (wrappers) {
			if (wrappers.containsKey(node))
				wrappers.remove(node);
		}
	}

	@Override
	public URL getLocation() {
		return root.getLocation();
	}

	@Override
	public ISecurePreferences getPreferences() {
		return wrapper(root);
	}

	public SecurePreferencesRoot getRootData() {
		return root;
	}

	//////////////////////////////////////////////////////////////////////////////////
	// Handling of options 

	@Override
	public boolean hasOption(Object key) {
		synchronized (options) {
			return options.containsKey(key);
		}
	}

	@Override
	public Object getOption(Object key) {
		synchronized (options) {
			return options.get(key);
		}
	}

	public Object setOption(Object key, Object value) {
		synchronized (options) {
			return options.put(key, value);
		}
	}

	public Object removeOption(Object key) {
		synchronized (options) {
			return options.remove(key);
		}
	}
}
