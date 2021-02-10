/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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

import org.eclipse.equinox.internal.security.storage.Base64;
import org.eclipse.equinox.internal.security.storage.SlashEncode;

/**
 * Collection of helper methods.
 * <p>
 * This class is not intended to be instantiated or extended by clients.
 * </p>
 */
final public class EncodingUtils {

	/**
	 * Encode strings containing forward slashes so that they can be used as node names 
	 * with secure preferences. It is the responsibility of the consumer to manually encode 
	 * such strings before attempting to obtain corresponding nodes from secure preferences.
	 * <p>
	 * Internally, the class uses a subset of JIT encoding. The forward slashes 
	 * and backward slashes are encoded.
	 * </p>
	 * @see #decodeSlashes(String)
	 * @param nodeName string to be encoded
	 * @return encoded string, <code>null</code> if argument was <code>null</code>
	 */
	static public String encodeSlashes(String nodeName) {
		return SlashEncode.encode(nodeName);
	}

	/**
	 * Decode strings previously encoded with the {@link #encodeSlashes(String)} method.
	 * @param nodeName string to be decoded
	 * @return decoded string, <code>null</code> if argument was <code>null</code>
	 */
	static public String decodeSlashes(String nodeName) {
		return SlashEncode.decode(nodeName);
	}

	/**
	 * Provides Base64 encoding of the data. This Base64 encoding does not insert 
	 * end-of-line characters (but can properly decode strings with EOLs inserted).
	 * @param bytes data to be encoded
	 * @return data encoded as Base64 string
	 */
	static public String encodeBase64(byte[] bytes) {
		return Base64.encode(bytes);
	}

	/**
	 * Provides decoding of Base64-encoded string
	 * @param string data encoded as Base64
	 * @return decoded data
	 */
	static public byte[] decodeBase64(String string) {
		return Base64.decode(string);
	}

}
