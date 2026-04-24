/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.p2maven.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.tycho.p2maven.transport.Response.ContentEncoding;
import org.eclipse.tycho.p2maven.transport.Response.ResponseConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * End-to-end test for the cache-hit detection path of
 * {@link SharedHttpCacheStorage}. Spins up a real HTTP server (via the
 * built-in {@link HttpServer}) and drives the storage with the real
 * {@link java.net.http.HttpClient}, so no production code is mocked.
 *
 * The test verifies that a {@link DownloadStatusOutputStream} registered on
 * the current thread is correctly flagged as {@code fromCache} when a
 * request is served from the local cache (either a plain hit or a 304
 * revalidation), and is left unflagged when the file is actually
 * transferred.
 */
class SharedHttpCacheStorageIntegrationTest {

	@TempDir
	Path cacheDir;

	private HttpServer server;
	private String baseUrl;
	private AtomicInteger requestCount;
	private volatile HttpHandler handler;

	private SharedHttpCacheStorage cache;
	private TestCacheConfig cacheConfig;
	private HttpTransportFactory transportFactory;
	private final Logger logger = new ConsoleLogger();

	@BeforeEach
	void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		requestCount = new AtomicInteger();
		server.createContext("/", exchange -> {
			requestCount.incrementAndGet();
			handler.handle(exchange);
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

		cacheConfig = new TestCacheConfig(cacheDir.toFile());
		cache = new SharedHttpCacheStorage();
		Field field = SharedHttpCacheStorage.class.getDeclaredField("cacheConfig");
		field.setAccessible(true);
		field.set(cache, cacheConfig);

		transportFactory = new JdkHttpTransportFactory();
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
		DownloadStatusOutputStream.clearCurrent();
	}

	@Test
	void realDownload_doesNotMarkFromCache() throws Exception {
		byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
		handler = exchange -> sendBody(exchange, 200, body, Map.of());

		DownloadStatusOutputStream stream = new DownloadStatusOutputStream(OutputStream.nullOutputStream(),
				"test");
		stream.setAsCurrent();
		try {
			File file = cache.getCacheEntry(URI.create(baseUrl + "/artifact.jar"), logger)
					.getCacheFile(transportFactory);
			assertEquals("hello", Files.readString(file.toPath()));
		} finally {
			DownloadStatusOutputStream.clearCurrent();
		}

		assertFalse(stream.isFromCache(), "real download must not be flagged as from-cache");
		assertEquals(1, requestCount.get());
	}

	@Test
	void freshCacheHit_marksFromCacheAndDoesNotHitServer() throws Exception {
		URI uri = URI.create(baseUrl + "/artifact.jar");

		// Prime the cache with a real download.
		byte[] body = "cached-body".getBytes(StandardCharsets.UTF_8);
		handler = exchange -> sendBody(exchange, 200, body, Map.of());
		cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);
		int countAfterPriming = requestCount.get();

		// Second access of the same URI should be served from the cache: the
		// configured minimum cache period (default 1h) has not elapsed.
		DownloadStatusOutputStream stream = new DownloadStatusOutputStream(OutputStream.nullOutputStream(),
				"test");
		stream.setAsCurrent();
		try {
			File file = cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);
			assertEquals("cached-body", Files.readString(file.toPath()));
		} finally {
			DownloadStatusOutputStream.clearCurrent();
		}

		assertTrue(stream.isFromCache(), "fresh cache hit must be flagged as from-cache");
		assertEquals(countAfterPriming, requestCount.get(), "no additional request should have been made");
	}

	@Test
	void revalidation_304_marksFromCache() throws Exception {
		URI uri = URI.create(baseUrl + "/content.xml");

		// Prime the cache with an ETag so the next request can be a conditional GET.
		byte[] body = "stale-body".getBytes(StandardCharsets.UTF_8);
		handler = exchange -> sendBody(exchange, 200, body, Map.of("ETag", "\"v1\""));
		cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);
		int countAfterPriming = requestCount.get();

		// Force revalidation on the next access and make the server answer 304.
		cacheConfig.update = true;
		handler = exchange -> sendBody(exchange, 304, new byte[0], Map.of());

		DownloadStatusOutputStream stream = new DownloadStatusOutputStream(OutputStream.nullOutputStream(),
				"test");
		stream.setAsCurrent();
		try {
			File file = cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);
			assertEquals("stale-body", Files.readString(file.toPath()));
		} finally {
			DownloadStatusOutputStream.clearCurrent();
		}

		assertTrue(stream.isFromCache(), "304 revalidation must be flagged as from-cache");
		assertEquals(countAfterPriming + 1, requestCount.get(),
				"exactly one conditional request should have been issued");
	}

	private static void sendBody(HttpExchange exchange, int status, byte[] body, Map<String, String> extraHeaders)
			throws IOException {
		extraHeaders.forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
		if (status == 304 || body.length == 0) {
			exchange.sendResponseHeaders(status, -1);
		} else {
			exchange.sendResponseHeaders(status, body.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(body);
			}
		}
		exchange.close();
	}

	private static final class TestCacheConfig implements TransportCacheConfig {
		private final File cacheLocation;
		boolean update;

		TestCacheConfig(File cacheLocation) {
			this.cacheLocation = cacheLocation;
		}

		@Override
		public boolean isOffline() {
			return false;
		}

		@Override
		public boolean isUpdate() {
			return update;
		}

		@Override
		public boolean isInteractive() {
			return true;
		}

		@Override
		public boolean isDebug() {
			return false;
		}

		@Override
		public File getCacheLocation() {
			return cacheLocation;
		}
	}

	/**
	 * Minimal real {@link HttpTransportFactory} backed by
	 * {@link java.net.http.HttpClient}. Intentionally free of the Plexus-wired
	 * proxy/authenticator logic so the test exercises {@link SharedHttpCacheStorage}
	 * against an actual HTTP server without additional test infrastructure.
	 */
	private static final class JdkHttpTransportFactory implements HttpTransportFactory {
		private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

		@Override
		public HttpTransport createTransport(URI uri) {
			return new JdkHttpTransport(client, uri);
		}
	}

	private static final class JdkHttpTransport implements HttpTransport {
		private final HttpClient client;
		private final Builder builder;
		private final URI uri;

		JdkHttpTransport(HttpClient client, URI uri) {
			this.client = client;
			this.uri = uri;
			this.builder = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(10));
		}

		@Override
		public void setHeader(String key, String value) {
			builder.setHeader(key, value);
		}

		@Override
		public <T> T get(ResponseConsumer<T> consumer) throws IOException {
			try {
				HttpResponse<java.io.InputStream> response = client.send(builder.GET().build(),
						BodyHandlers.ofInputStream());
				try (JdkResponse wrapper = new JdkResponse(response)) {
					return consumer.handleResponse(wrapper);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}

		@Override
		public Headers head() throws IOException {
			try {
				HttpResponse<Void> response = client.send(
						builder.method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), BodyHandlers.discarding());
				return new JdkHeaders(response);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}
	}

	private static class JdkHeaders implements Headers {
		protected final HttpResponse<?> response;

		JdkHeaders(HttpResponse<?> response) {
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
			return response.headers().firstValue(LAST_MODIFIED_HEADER).map(v -> 0L).orElse(0L);
		}

		@Override
		public void close() {
			// nothing
		}
	}

	private static final class JdkResponse extends JdkHeaders implements Response {
		JdkResponse(HttpResponse<java.io.InputStream> response) {
			super(response);
		}

		@Override
		public void transferTo(OutputStream outputStream, ContentEncoding transportEncoding) throws IOException {
			@SuppressWarnings("unchecked")
			HttpResponse<java.io.InputStream> r = (HttpResponse<java.io.InputStream>) response;
			try (java.io.InputStream body = r.body()) {
				if (body != null) {
					transportEncoding.decode(body).transferTo(outputStream);
				}
			}
		}

		@Override
		public void close() {
			@SuppressWarnings("unchecked")
			HttpResponse<java.io.InputStream> r = (HttpResponse<java.io.InputStream>) response;
			try (java.io.InputStream body = r.body()) {
				// drain and close
			} catch (IOException e) {
				// ignore
			}
		}
	}
}
