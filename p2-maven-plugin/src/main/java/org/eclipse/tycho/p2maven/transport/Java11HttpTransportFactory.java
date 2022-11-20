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
import java.io.InterruptedIOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;

@Component(role = HttpTransportFactory.class, hint = Java11HttpTransportFactory.HINT)
public class Java11HttpTransportFactory implements HttpTransportFactory, Initializable {

	static final String HINT = "Java11Http";
	@Requirement
	ProxyHelper proxyHelper;
	@Requirement
	MavenAuthenticator authenticator;

	private HttpClient client;

	@Override
	public HttpTransport createTransport(URI uri) {
		Java11HttpTransport transport = new Java11HttpTransport(client, HttpRequest.newBuilder().uri(uri));
		authenticator.preemtiveAuth(transport, uri);
		return transport;
	}

	private static final class Java11HttpTransport implements HttpTransport {

		private Builder builder;
		private HttpClient client;

		public Java11HttpTransport(HttpClient client, Builder builder) {
			this.client = client;
			this.builder = builder;
		}

		@Override
		public void setHeader(String key, String value) {
			builder.setHeader(key, value);
		}

		@Override
		public Response<InputStream> get() throws IOException {
			try {
				HttpResponse<InputStream> response = client.send(builder.GET().build(), BodyHandlers.ofInputStream());
				return new ResponseImplementation<>(response) {

					@Override
					public void close() {
						try (InputStream stream = body()) {
						} catch (IOException e) {
							// don't care...
						}
					}
				};
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

		@Override
		public Response<Void> head() throws IOException {
			try {
				HttpResponse<Void> response = client.send(builder.method("HEAD", null).build(),
						BodyHandlers.discarding());
				return new ResponseImplementation<>(response) {
					@Override
					public void close() {
						// nothing...
					}
				};
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

	}

	private static abstract class ResponseImplementation<T> implements Response<T> {
		private final HttpResponse<T> response;

		private ResponseImplementation(HttpResponse<T> response) {
			this.response = response;
		}

		@Override
		public int statusCode() {
			return response.statusCode();
		}

		@Override
		public Map<String, List<String>> headers() {
			return response.headers().map();
		}

		@Override
		public T body() {
			return response.body();
		}

		@Override
		public String getHeader(String header) {
			return response.headers().firstValue(header).orElse(null);
		}
	}

	@Override
	public void initialize() throws InitializationException {
		client = HttpClient.newBuilder().followRedirects(Redirect.NEVER).authenticator(authenticator)
				.proxy(new ProxySelector() {

					@Override
					public List<Proxy> select(URI uri) {
						Proxy proxy = proxyHelper.getProxy(uri);
						return List.of(proxy);
					}

					@Override
					public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
						// anything useful we can do here?

					}
				}).build();

	}

}
