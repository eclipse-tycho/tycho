/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

public interface HttpTransportFactory {

	static final long TIMEOUT_SECONDS = Long.getLong("tycho.http.transport.timeout", 30);

	/**
	 * Maximum number of retries for transient HTTP errors (502/503/504) before
	 * failing. Configurable via system property {@code tycho.http.transport.retry.count}.
	 */
	static final int HTTP_RETRY_COUNT = Math.max(0,
			Integer.getInteger("tycho.http.transport.retry.count", 3));

	/**
	 * Initial delay in seconds before the first retry on a transient HTTP error.
	 * Subsequent retries multiply this by the attempt number (linear back-off).
	 * Configurable via system property {@code tycho.http.transport.retry.initial-delay}.
	 */
	static final long HTTP_RETRY_INITIAL_DELAY_SECONDS = Math.max(0L,
			Long.getLong("tycho.http.transport.retry.initial-delay", 5));

	HttpTransport createTransport(URI uri);

}
