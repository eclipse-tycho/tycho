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

import java.io.IOException;
import java.net.URI;

/**
 * Thrown when an HTTP request receives a transient server-side error response
 * (e.g. 502, 503, 504) that may succeed on retry.
 */
class TransientHttpException extends IOException {

    private final int statusCode;

    TransientHttpException(int statusCode, URI uri) {
        super("Server returned HTTP code: " + statusCode + " for URL " + uri);
        this.statusCode = statusCode;
    }

    int getStatusCode() {
        return statusCode;
    }

}
