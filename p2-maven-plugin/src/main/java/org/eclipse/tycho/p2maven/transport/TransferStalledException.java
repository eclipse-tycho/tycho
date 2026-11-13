/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2maven.transport;

import java.net.URI;
import java.net.http.HttpTimeoutException;

/**
 * Thrown when no data was received for a download within the configured
 * timeout. It is a {@link HttpTimeoutException} so it is reported to p2 like any
 * other timeout, which allows p2 to try another mirror instead of retrying a
 * server that is likely not going to recover.
 */
class TransferStalledException extends HttpTimeoutException {

	TransferStalledException(URI uri, long timeoutSeconds, Throwable cause) {
		super("No data received for " + timeoutSeconds + " seconds while downloading " + uri);
		initCause(cause);
	}

}
