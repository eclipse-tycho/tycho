/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
 * This is an implementation of Base64 encoding allowing byte sequences to be
 * converted into strings - safe to be stored in basic Java structures.
 * <p>
 * This Base64 encoding does not insert end-of-line characters
 * (but can properly decode strings with EOLs inserted).
 * </p>
 */
public class Base64 {

	final static private char[] encodeTable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray(); //$NON-NLS-1$

	final static private byte BASE64_PADDING = 126;
	final static private byte BASE64_INVALID = 127;

	final static private byte[] decodeTable = new byte[256];

	static {
		for (int i = 0; i < 256; i++)
			decodeTable[i] = BASE64_INVALID;

		for (int i = 0; i < 64; i++)
			decodeTable[encodeTable[i]] = (byte) i;

		decodeTable['='] = BASE64_PADDING;
	}

	static private byte decode(char c) {
		if (c >= 256)
			throw new IllegalArgumentException();
		return decodeTable[c];
	}

	static public byte[] decode(String str) {
		if (str == null)
			return null;

		// eliminate all unexpected characters (might have EOLs inserted)
		char[] source = str.toCharArray();
		int originalSize = source.length;
		char[] tmp = new char[originalSize];
		int count = 0;
		for (int i = 0; i < originalSize; i++) {
			if (decode(source[i]) != BASE64_INVALID)
				tmp[count++] = source[i];
		}
		char[] chars = new char[count];
		System.arraycopy(tmp, 0, chars, 0, count);

		int size = chars.length;
		byte[] result = new byte[size];
		int pos = 0;
		for (int i = 0; i < size; i += 4) {
			byte group1 = decode(chars[i]);
			byte group2 = (i + 1 < size) ? decode(chars[i + 1]) : 0;
			byte group3 = (i + 2 < size) ? decode(chars[i + 2]) : 0;
			byte group4 = (i + 3 < size) ? decode(chars[i + 3]) : 0;

			result[pos++] = (byte) ((group1 << 2) | (group2 >> 4));
			if (group3 != BASE64_PADDING)
				result[pos++] = (byte) (((group2 & 0xF) << 4) | (group3 >> 2));
			if (group4 != BASE64_PADDING)
				result[pos++] = (byte) (((group3 & 0x3) << 6) | group4);
		}

		byte[] output = new byte[pos];
		System.arraycopy(result, 0, output, 0, pos);
		return output;
	}

	static public String encode(byte[] bytes) {
		if (bytes == null)
			return null;
		char[] longResult = new char[bytes.length * 2 + 2];
		int pos = 0;
		for (int i = 0; i < bytes.length; i += 3) {
			int byte1 = 0xFF & bytes[i];
			int byte2 = (i + 1 < bytes.length) ? 0xFF & bytes[i + 1] : 0;
			int byte3 = (i + 2 < bytes.length) ? 0xFF & bytes[i + 2] : 0;

			int group1 = byte1 >> 2;
			int group2 = ((byte1 & 0x3) << 4) | (byte2 >> 4);
			int group3 = ((byte2 & 0xF) << 2) | (byte3 >> 6);
			int group4 = byte3 & 0x3F;

			longResult[pos++] = encodeTable[group1];
			longResult[pos++] = encodeTable[group2];

			longResult[pos++] = (i + 1 < bytes.length) ? encodeTable[group3] : '=';
			longResult[pos++] = (i + 2 < bytes.length) ? encodeTable[group4] : '=';
		}

		char[] result = new char[pos];
		System.arraycopy(longResult, 0, result, 0, pos);
		return new String(result);
	}

}
