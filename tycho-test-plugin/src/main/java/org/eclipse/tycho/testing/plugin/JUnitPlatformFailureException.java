/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.testing.plugin;

public class JUnitPlatformFailureException extends Exception {
	private static final long serialVersionUID = 1L;

	public JUnitPlatformFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	public JUnitPlatformFailureException(String message) {
		super(message);
	}

}
