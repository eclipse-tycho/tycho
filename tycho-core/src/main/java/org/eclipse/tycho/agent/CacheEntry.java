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
package org.eclipse.tycho.agent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.tycho.MavenRepositorySettings.Credentials;

public interface CacheEntry {

    long getLastModified(IProxyService proxyService, Function<URI, Credentials> credentialsProvider) throws IOException;

    File getCacheFile(IProxyService proxyService, Function<URI, Credentials> credentialsProvider) throws IOException;
}
