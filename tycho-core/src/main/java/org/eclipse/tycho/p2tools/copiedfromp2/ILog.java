/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.director.app;

import org.eclipse.core.runtime.IStatus;

/**
 * Manages all outputs of the director application: logs to a file as well as the standard streams
 * <p>
 * This indirection is needed in order to manage the outputs when the director is called from ant, where
 * the standard streams are handled differently.
 */
public interface ILog {
	/**
	 * Send status to the standard log
	 */
	void log(IStatus status);

	/**
	 *
	 * @deprecated Use {@link ILog#printOut()} or {@link ILog#printErr()}
	 */
	@Deprecated
	default void log(String message) {
		printOut(message);
	}

	/**
	 * Notify that logging is completed & cleanup resources
	 */
	void close();

	/**
	 * Print status on stdout or stderr.
	 *
	 * By default calls {@link #log}
	 */
	default void printOut(String line) {
		System.out.println(line);
	}

	/**
	 * Send line to stdout
	 *
	 * By default does nothing
	 *
	 * @param line Message line
	 */
	default void printErr(String line) {
		System.err.println(line);
	}
}
