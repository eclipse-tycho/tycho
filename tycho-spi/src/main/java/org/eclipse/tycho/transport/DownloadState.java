/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.transport;

/**
 * Describes how a {@link TransportProtocolHandler} obtained a {@link FileState},
 * which allows callers to distinguish real downloads from cache hits without
 * being tied to a specific transport or caching technique.
 */
public enum DownloadState {
	/** The resource was actually transferred from the remote location. */
	DOWNLOADED,
	/** The resource was served from a previously cached copy. */
	FROM_CACHE,
	/** The cached copy was revalidated against the remote and is still up-to-date. */
	NOT_MODIFIED,
	/** The resource is a local file and was not transferred at all. */
	LOCAL_FILE;

	/**
	 * @return {@code true} if no real download happened, i.e. the resource was
	 *         served locally or from a cache.
	 */
	public boolean isFromCache() {
		return this != DOWNLOADED;
	}
}
