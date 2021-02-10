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

import org.eclipse.core.runtime.IPath;

/**
 * If the key contains a slash character then we must use a double-slash to indicate 
 * the end of the path/the beginning of the key.
 */
public class PersistedPath {

	private static final String DOUBLE_SLASH = "//"; //$NON-NLS-1$

	final private String key;
	final private String path;

	public PersistedPath(String path, String key) {
		this.key = key;
		this.path = path;
	}

	public String getKey() {
		return key;
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		String result;
		int pathLength = path == null ? 0 : path.length();
		if (key.indexOf(IPath.SEPARATOR) == -1) {
			if (pathLength == 0)
				result = key;
			else
				result = path + IPath.SEPARATOR + key;
		} else {
			if (pathLength == 0)
				result = DOUBLE_SLASH + key;
			else
				result = path + DOUBLE_SLASH + key;
		}
		return result;
	}

	public PersistedPath(String fullPath) {
		// check to see if we have an indicator which tells us where the path ends
		int index = fullPath.indexOf(DOUBLE_SLASH);
		if (index == -1) {
			// we don't have a double-slash telling us where the path ends 
			// so the path is up to the last slash character
			int lastIndex = fullPath.lastIndexOf(IPath.SEPARATOR);
			if (lastIndex == -1) {
				path = null;
				key = fullPath;
			} else {
				path = fullPath.substring(0, lastIndex);
				key = fullPath.substring(lastIndex + 1);
			}
		} else {
			// the child path is up to the double-slash and the key
			// is the string after it
			path = fullPath.substring(0, index);
			key = fullPath.substring(index + 2);
		}

		// XXX is this needed? 
		// adjust if we have an absolute path
		//		if (path != null)
		//			if (path.length() == 0)
		//				path = null;
		//			else if (path.charAt(0) == IPath.SEPARATOR)
		//				path = path.substring(1);
	}

}
