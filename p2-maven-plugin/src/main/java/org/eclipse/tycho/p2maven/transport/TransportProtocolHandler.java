package org.eclipse.tycho.p2maven.transport;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public interface TransportProtocolHandler {

	long getLastModified(URI uri) throws IOException;

	File getFile(URI remoteFile) throws IOException;

}
