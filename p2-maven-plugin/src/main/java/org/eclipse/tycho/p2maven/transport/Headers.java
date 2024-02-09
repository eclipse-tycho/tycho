/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface Headers extends AutoCloseable {

	String ENCODING_IDENTITY = "identity";
	String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	String HEADER_CONTENT_ENCODING = "Content-Encoding";
	String ENCODING_GZIP = "gzip";
	String ETAG_HEADER = "ETag";
	String LAST_MODIFIED_HEADER = "Last-Modified";
	String EXPIRES_HEADER = "Expires";
	String CACHE_CONTROL_HEADER = "Cache-Control";
	String MAX_AGE_DIRECTIVE = "max-age";
	String MUST_REVALIDATE_DIRECTIVE = "must-revalidate";

	int statusCode() throws IOException;

	Map<String, List<String>> headers();

	@Override
	void close();

	URI getURI();

	String getHeader(String header);

	long getLastModified();

	default void checkResponseCode() throws FileNotFoundException, IOException {
		int code = statusCode();
		if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
			if (code == HttpURLConnection.HTTP_NOT_FOUND || code == HttpURLConnection.HTTP_GONE) {
				throw new FileNotFoundException(getURI().toString());
			} else {
				throw new java.io.IOException("Server returned HTTP code: " + code + " for URL " + getURI().toString());
			}
		}
	}

}
