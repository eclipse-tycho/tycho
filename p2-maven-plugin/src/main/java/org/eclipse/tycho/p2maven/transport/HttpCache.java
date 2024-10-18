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

import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.net.URI;

public interface HttpCache {

	/**
	 * Fetches the cache entry for this URI
	 * 
	 * @param uri
	 * @return
	 * @throws FileNotFoundException
	 *             if the URI is know to be not found
	 */
	CacheEntry getCacheEntry(URI uri, Logger logger) throws FileNotFoundException;

}