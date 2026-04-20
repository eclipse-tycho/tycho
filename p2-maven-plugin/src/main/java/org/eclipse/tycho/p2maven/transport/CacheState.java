/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.p2maven.transport;

/**
 * Describes the outcome of a {@link HttpCache} access, so that callers can
 * produce accurate log output (e.g. distinguish "Downloading" from "Fetched
 * from cache").
 */
public enum CacheState {
	/** The file was served from the local cache without any network access. */
	FROM_CACHE,
	/** A conditional request was performed and the server answered 304 Not Modified. */
	NOT_MODIFIED,
	/** The file was downloaded (full 2xx response with body). */
	DOWNLOADED,
	/** No information available (e.g. non-HTTP transport or not yet accessed). */
	UNKNOWN
}
