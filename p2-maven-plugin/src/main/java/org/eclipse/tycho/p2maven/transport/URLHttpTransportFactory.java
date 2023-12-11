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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;

@Component(role = HttpTransportFactory.class, hint = URLHttpTransportFactory.HINT)
public class URLHttpTransportFactory implements HttpTransportFactory {

	static final String HINT = "JavaUrl";
	@Requirement
	ProxyHelper proxyHelper;
	@Requirement
	MavenAuthenticator authenticator;

	@Override
	public HttpTransport createTransport(URI uri) {
		URLHttpTransport transport = new URLHttpTransport(uri, proxyHelper, authenticator);
		return transport;
	}

	private static final class URLHttpTransport implements HttpTransport {

		private URI uri;
		private ProxyHelper proxyHelper;
		private MavenAuthenticator authenticator;
		private Map<String, String> extraHeaders = new HashMap<>();

		public URLHttpTransport(URI uri, ProxyHelper proxyHelper, MavenAuthenticator authenticator) {
			this.uri = uri;
			this.proxyHelper = proxyHelper;
			this.authenticator = authenticator;
		}

		@Override
		public void setHeader(String key, String value) {
			extraHeaders.put(key, value);
		}

		@Override
		public Response<InputStream> get() throws IOException {
			HttpURLConnection connection = createConnection();
			connection.connect();
			return new HttpResponse<>(connection) {

				@Override
				public void close() {
					// discard any remaining data and close the stream to return the connection to
					// the pool..
					try (InputStream stream = anyBody()) {
						for (int i = 0; i < 1024 * 10; i++) {
							int read = stream.read();
							if (read < 0) {
								break;
							}
						}
					} catch (IOException e) {
						// don't care...
					}
				}

				private InputStream anyBody() throws IOException {
					InputStream errorStream = connection.getErrorStream();
					if (errorStream != null) {
						return errorStream;
					}
					return connection.getInputStream();
				}

				@Override
				public InputStream body() throws IOException {
					return connection.getInputStream();
				}

			};
		}

		private HttpURLConnection createConnection() throws IOException, MalformedURLException {
			HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxyHelper.getProxy(uri));
			connection.setAuthenticator(authenticator);
			connection.setInstanceFollowRedirects(false);
			authenticator.preemtiveAuth((k, v) -> connection.setRequestProperty(k, v), uri);
			extraHeaders.forEach(connection::setRequestProperty);
			return connection;
		}

		@Override
		public Response<Void> head() throws IOException {
			HttpURLConnection connection = createConnection();
			connection.setRequestMethod("HEAD");
			connection.connect();
			return new HttpResponse<>(connection) {

				@Override
				public void close() {
					// HEAD returns no body...
				}

				@Override
				public Void body() throws IOException {
					return null;
				}
			};
		}
	}

	private static abstract class HttpResponse<T> implements Response<T> {

		private HttpURLConnection connection;

		HttpResponse(HttpURLConnection connection) {
			this.connection = connection;
		}

		@Override
		public int statusCode() throws IOException {
			return connection.getResponseCode();
		}

		@Override
		public Map<String, List<String>> headers() {
			return connection.getHeaderFields();
		}

		@Override
		public String getHeader(String header) {
			return connection.getHeaderField(header);
		}

		@Override
		public long getLastModified() {
			return connection.getLastModified();
		}

		@Override
		public URI getURI() {
			try {
				return connection.getURL().toURI();
			} catch (URISyntaxException e) {
				throw new AssertionError("Should never happen!", e);
			}
		}

	}

}
