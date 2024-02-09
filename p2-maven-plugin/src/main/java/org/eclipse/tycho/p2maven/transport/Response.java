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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

public interface Response extends Headers {

	default void transferTo(OutputStream outputStream) throws IOException {
		String encoding = getHeader(Headers.HEADER_CONTENT_ENCODING);
		if (Headers.ENCODING_GZIP.equals(encoding)) {
			transferTo(outputStream, GZIPInputStream::new);
		} else if (encoding == null || encoding.isEmpty() || Headers.ENCODING_IDENTITY.equals(encoding)) {
			transferTo(outputStream, stream -> stream);
		} else {
			throw new IOException("Unknown content encoding: " + encoding);
		}
	}

	void transferTo(OutputStream outputStream, ContentEncoding transportEncoding) throws IOException;

	interface ContentEncoding {
		InputStream decode(InputStream raw) throws IOException;
	}

	interface ResponseConsumer<T> {
		T handleResponse(Response response) throws IOException;
	}

}
