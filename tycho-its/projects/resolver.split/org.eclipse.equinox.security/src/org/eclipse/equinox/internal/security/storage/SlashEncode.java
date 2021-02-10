/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

/**
 * Utility class to encode forward slash characters so that strings containing
 * forward slashes can be used as node names with secure preferences. It is
 * the responsibility of the consumer to manually encode such strings before
 * attempting to obtain corresponding nodes from secure preferences.
 * <p>
 * Internally, the class uses a subset of JIT encoding. The forward slashes 
 * and backward slashes are encoded.
 * </p><p>
 * This class is not intended to be instantiated or subclassed by users.
 * </p>  
 */
final public class SlashEncode {

	final private static char SLASH = '/';
	final private static char BACK_SLASH = '\\';

	final private static String ENCODED_SLASH = "\\2f"; //$NON-NLS-1$
	final private static String ENCODED_BACK_SLASH = "\\5c"; //$NON-NLS-1$

	static public String decode(String str) {
		if (str == null)
			return null;
		int size = str.length();
		if (size == 0)
			return str;

		StringBuffer processed = new StringBuffer(size);
		int processedPos = 0;

		for (int i = 0; i < size; i++) {
			char c = str.charAt(i);
			if (c == BACK_SLASH) {
				if (i + 2 >= size)
					continue;
				String encoded = str.substring(i, i + 3);
				char decoded = 0;
				if (ENCODED_SLASH.equals(encoded))
					decoded = SLASH;
				else if (ENCODED_BACK_SLASH.equals(encoded))
					decoded = BACK_SLASH;
				if (decoded == 0)
					continue;
				if (i > processedPos)
					processed.append(str.substring(processedPos, i));
				processed.append(decoded);
				processedPos = i + 3;
				i += 2; // skip over remaining encoded portion
			}
		}
		if (processedPos == 0)
			return str;
		if (processedPos < size)
			processed.append(str.substring(processedPos));
		return new String(processed);
	}

	static public String encode(String str) {
		if (str == null)
			return null;
		int size = str.length();
		if (size == 0)
			return str;

		StringBuffer processed = new StringBuffer(size);
		int processedPos = 0;

		for (int i = 0; i < size; i++) {
			char c = str.charAt(i);
			if (c == SLASH || c == BACK_SLASH) {
				if (i > processedPos)
					processed.append(str.substring(processedPos, i));
				if (c == SLASH)
					processed.append(ENCODED_SLASH);
				else if (c == BACK_SLASH)
					processed.append(ENCODED_BACK_SLASH);
				processedPos = i + 1;
			}
		}
		if (processedPos == 0)
			return str;
		if (processedPos < size)
			processed.append(str.substring(processedPos));
		return new String(processed);
	}

}
