package org.eclipse.tycho.p2maven.transport;

import java.io.IOException;
import java.io.InputStream;

public interface HttpTransport {

	void setHeader(String key, String value);

	Response<InputStream> get() throws IOException;

	Response<Void> head() throws IOException;

}
