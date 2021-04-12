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

/**
 * This class describes an exception that could be produced by the secure 
 * preferences. Depending on the error code, callers might be able to mitigate
 * the originating problems and re-try the operation (for instance, if incorrect 
 * password was provided or in case a required storage module was not available).
 * <p>
 * This class is not intended to be instantiated or extended by clients.
 * </p>
 */
final public class StorageException extends Exception {

	private static final long serialVersionUID = -6352767405585664435L;

	/**
	 * Internal error due to a problem in setup or internal implementation.
	 */
	final public static int INTERNAL_ERROR = 0;

	/**
	 * No appropriate password provider module is available.
	 */
	final public static int NO_SECURE_MODULE = 1;

	/**
	 * Error occurred during the encryption process.
	 * <p> 
	 * Such error might have being created by using inappropriate key, for instance, using key 
	 * that is too strong for the cryptographic policy in JVM.
	 * </p>
	 */
	final public static int ENCRYPTION_ERROR = 2;

	/**
	 * Error occurred during the decryption process.
	 * <p>
	 * This error might be caused by an incorrect password or corrupted data.
	 * </p>
	 */
	final public static int DECRYPTION_ERROR = 3;

	/**
	 * Secure preferences were unable to retrieve the password.
	 */
	final public static int NO_PASSWORD = 4;

	final private int errorCode;

	public StorageException(int errorCode, Throwable exception) {
		super(exception.getMessage(), exception);
		this.errorCode = errorCode;
	}

	public StorageException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
