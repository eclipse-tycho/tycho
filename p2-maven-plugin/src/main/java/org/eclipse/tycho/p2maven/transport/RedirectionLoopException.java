/*******************************************************************************
 * Copyright (c) 2025 Tobias Hahnen and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Hahnen - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.transport;

import java.io.IOException;

/**
 * Exception thrown when a URI would be redirecting to itself. This will cause a
 * loop and therefore might lead to a StackOverflowError.
 */
public class RedirectionLoopException extends IOException {
	public RedirectionLoopException(String uri) {
		super(uri);
	}
}
