package org.eclipse.tycho.p2maven.transport;

import java.io.IOException;

import org.eclipse.tycho.p2maven.transport.Response.ResponseConsumer;

public interface HttpTransport {

	void setHeader(String key, String value);

	<T> T get(ResponseConsumer<T> consumer) throws IOException;

	Headers head() throws IOException;

}
