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

import java.nio.file.Path;

/**
 * The result of resolving a resource through a {@link TransportProtocolHandler}:
 * the local {@link Path} the resource is available at together with the
 * {@link DownloadState} describing how it was obtained.
 *
 * @param file  the local file the resource is available at
 * @param state how the file was obtained
 */
public record FileState(Path file, DownloadState state) {
}
