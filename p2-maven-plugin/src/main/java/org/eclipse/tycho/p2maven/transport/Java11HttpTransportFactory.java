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
import java.io.OutputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.eclipse.tycho.p2maven.helper.ProxyHelper;
import org.eclipse.tycho.p2maven.transport.Response.ResponseConsumer;

/**
 * A transport using Java11 HttpClient
 */
@Component(role = HttpTransportFactory.class, hint = Java11HttpTransportFactory.HINT)
public class Java11HttpTransportFactory implements HttpTransportFactory, Initializable {
	private static final int MAX_DISCARD = 1024 * 10;
	private static final byte[] DUMMY_BUFFER = new byte[MAX_DISCARD];

	// see https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
	// per RFC there are three different formats:
	private static final List<ThreadLocal<DateFormat>> DATE_PATTERNS = List.of(//
			// RFC 1123
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)),
			// RFC 1036
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH)),
			// ANSI C's asctime() format
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE MMMd HH:mm:ss yyyy", Locale.ENGLISH)));

	static final String HINT = "Java11Client";
	@Requirement
	ProxyHelper proxyHelper;
	@Requirement
	MavenAuthenticator authenticator;
	@Requirement
	Logger logger;

	private HttpClient client;
	private HttpClient clientHttp1;

	@Override
	public HttpTransport createTransport(URI uri) {
		Java11HttpTransport transport = new Java11HttpTransport(client, clientHttp1, HttpRequest.newBuilder().uri(uri),
				uri, logger);
		authenticator.preemtiveAuth((k, v) -> transport.setHeader(k, v), uri);
		return transport;
	}

	private static final class Java11HttpTransport implements HttpTransport {

		private Builder builder;
		private HttpClient client;
		private Logger logger;
		private HttpClient clientHttp1;
		private URI uri;

		public Java11HttpTransport(HttpClient client, HttpClient clientHttp1, Builder builder, URI uri, Logger logger) {
			this.client = client;
			this.clientHttp1 = clientHttp1;
			this.builder = builder;
			this.uri = uri;
			this.logger = logger;
		}

		@Override
		public void setHeader(String key, String value) {
			builder.setHeader(key, value);
		}

		@Override
		public <T> T get(ResponseConsumer<T> consumer) throws IOException {
			try {
				try {
					return performGet(consumer, client);
				} catch (IOException e) {
					if (isGoaway(e)) {
						logger.info("Received GOAWAY from server " + uri.getHost() + " will retry download of " + uri
								+ " with Http/1...");
						TimeUnit.SECONDS.sleep(1);
						return performGet(consumer, clientHttp1);
					}
					throw e;
				}
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

		private <T> T performGet(ResponseConsumer<T> consumer, HttpClient httpClient)
				throws IOException, InterruptedException {
			HttpRequest request = builder.GET().timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
			HttpResponse<InputStream> response = httpClient.send(request, BodyHandlers.ofInputStream());
			try (ResponseImplementation<InputStream> implementation = new ResponseImplementation<>(response) {

				@Override
				public void close() {
					if (response.version() == Version.HTTP_1_1) {
						// discard any remaining data and close the stream to return the connection to
						// the pool..
						try (InputStream stream = response.body()) {
							int discarded = 0;
							while (discarded < MAX_DISCARD) {
								int read = stream.read(DUMMY_BUFFER);
								if (read < 0) {
									break;
								}
								discarded += read;
							}
						} catch (IOException e) {
							// don't care...
						}
					} else {
						// just closing should be enough to signal to the framework...
						try (InputStream stream = response.body()) {
						} catch (IOException e) {
							// don't care...
						}
					}
				}

				@Override
				public void transferTo(OutputStream outputStream, ContentEncoding transportEncoding)
						throws IOException {
					transportEncoding.decode(response.body()).transferTo(outputStream);
				}
			}) {
				return consumer.handleResponse(implementation);
			}
		}

		@Override
		public Response head() throws IOException {
			try {
				try {
					return doHead(client);
				} catch (IOException e) {
					if (isGoaway(e)) {
						logger.debug("Received GOAWAY from server " + uri.getHost()
								+ " will retry with Http/1...");
						TimeUnit.SECONDS.sleep(1);
						return doHead(clientHttp1);
					}
					throw e;
				}
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

		private Response doHead(HttpClient httpClient) throws IOException, InterruptedException {
			HttpResponse<Void> response = httpClient.send(
					builder.method("HEAD", BodyPublishers.noBody()).timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
							.build(),
					BodyHandlers.discarding());
			return new ResponseImplementation<>(response) {
				@Override
				public void close() {
					// nothing...
				}

				@Override
				public void transferTo(OutputStream outputStream, ContentEncoding transportEncoding)
						throws IOException {
					throw new IOException("HEAD returns no body");
				}
			};
		}

	}

	private static abstract class ResponseImplementation<T> implements Response {
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
		public String getHeader(String header) {
			return response.headers().firstValue(header).orElse(null);
		}

		@Override
		public URI getURI() {
			return response.uri();
		}

		@Override
		public long getLastModified() {
			String lastModifiedHeader = getHeader(LAST_MODIFIED_HEADER);
			if (lastModifiedHeader == null)
				return 0L;
			// first check if there are any quotes around and remove them
			if (lastModifiedHeader.length() > 1 && lastModifiedHeader.startsWith("'")
					&& lastModifiedHeader.endsWith("'")) {
				lastModifiedHeader = lastModifiedHeader.substring(1, lastModifiedHeader.length() - 1);
			}
			// no check all date formats
			for (final ThreadLocal<DateFormat> dateFormat : DATE_PATTERNS) {
				try {
					return dateFormat.get().parse(lastModifiedHeader).getTime();
				} catch (ParseException e) {
					// try next one...
				}
			}
			return 0L;
		}
	}

	@Override
	public void initialize() throws InitializationException {
		ProxySelector proxySelector = new ProxySelector() {

			@Override
			public List<Proxy> select(URI uri) {
				Proxy proxy = proxyHelper.getProxy(uri);
				return List.of(proxy);
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				// anything useful we can do here?

			}
		};
		client = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(TIMEOUT_SECONDS))
				.followRedirects(Redirect.NEVER)
				.proxy(proxySelector).build();
		clientHttp1 = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(TIMEOUT_SECONDS))
				.version(Version.HTTP_1_1).followRedirects(Redirect.NEVER)
				.proxy(proxySelector).build();

	}

	private static boolean isGoaway(Throwable e) {
		if (e == null) {
			return false;
		}
		if (e instanceof IOException) {
			// first check the message
			String message = e.getMessage();
			if (message != null && message.contains("GOAWAY received")) {
				return true;
			}
			// maybe it is in the stack?!?
			for (StackTraceElement stack : e.getStackTrace()) {
				if ("jdk.internal.net.http.Http2Connection.handleGoAway".equals(stack.getMethodName())) {
					return true;
				}
			}
		}
		// look further in the chain...
		return isGoaway(e.getCause());
	}

}
