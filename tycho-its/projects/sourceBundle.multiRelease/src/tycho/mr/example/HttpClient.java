package tycho.mr.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HttpClient {

	public byte[] fetchBytes(URL url) throws IOException {
		try (InputStream stream = url.openStream()) {
			// For Java < 9 we need to manually read the stream
			return readAllBytes(stream);
		}
	}
	
	private byte[] readAllBytes(InputStream stream) throws IOException {
		byte[] buffer = new byte[8192];
		int bytesRead;
		java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
		while ((bytesRead = stream.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		return output.toByteArray();
	}
}
