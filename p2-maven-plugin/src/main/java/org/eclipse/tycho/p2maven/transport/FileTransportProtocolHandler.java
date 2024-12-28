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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.transport.TransportProtocolHandler;

@Component(role = TransportProtocolHandler.class, hint = "file")
public class FileTransportProtocolHandler implements TransportProtocolHandler {

	@Override
	public long getLastModified(URI uri) throws IOException {
		return getFile(uri).lastModified();
	}


	@Override
	public File getFile(URI remoteFile) throws IOException {
		try {
			return new File(remoteFile);
		} catch (IllegalArgumentException e) {
			throw new IOException("Not a valid file URI: " + remoteFile);
		}
	}

}
