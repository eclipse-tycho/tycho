package tycho.mr.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HttpClient {

	public byte[] fetchBytes(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			// For Java >= 9 we can use the built-in readAllBytes
			return stream.readAllBytes();
		}
	}
}
