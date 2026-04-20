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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Properties;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.p2maven.transport.Response.ResponseConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that {@link SharedHttpCacheStorage} emits the correct log message
 * for the three possible outcomes of a cache access: pure cache hit, 304
 * revalidation, and real download.
 */
class SharedHttpCacheStorageLoggingTest {

	@TempDir
	Path cacheDir;

	private SharedHttpCacheStorage cache;
	private TransportCacheConfig cacheConfig;
	private HttpTransportFactory transportFactory;
	private HttpTransport transport;

	@BeforeEach
	void setUp() throws Exception {
		cacheConfig = mock(TransportCacheConfig.class);
		when(cacheConfig.getCacheLocation()).thenReturn(cacheDir.toFile());
		when(cacheConfig.isInteractive()).thenReturn(true);
		when(cacheConfig.isOffline()).thenReturn(false);
		when(cacheConfig.isUpdate()).thenReturn(false);
		when(cacheConfig.isDebug()).thenReturn(false);

		cache = new SharedHttpCacheStorage();
		Field field = SharedHttpCacheStorage.class.getDeclaredField("cacheConfig");
		field.setAccessible(true);
		field.set(cache, cacheConfig);

		transport = mock(HttpTransport.class);
		transportFactory = mock(HttpTransportFactory.class);
		when(transportFactory.createTransport(any(URI.class))).thenReturn(transport);
	}

	@Test
	void cacheHit_logsFetchedFromCache_andDoesNotCallTransport() throws Exception {
		URI uri = URI.create("https://example.org/site/p2.index");
		primeFreshCacheEntry(uri, "cached-body");

		Logger logger = mock(Logger.class);
		File file = cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);

		assertEquals("cached-body", Files.readString(file.toPath()));
		assertEquals(CacheState.FROM_CACHE, cache.getLastCacheState(uri));
		verify(logger, atLeastOnce()).debug(contains("Fetched from cache"));
		verify(logger, never()).info(contains("Downloading from"));
		verify(transport, never()).get(any());
	}

	@Test
	void revalidation_304_logsUpToDate() throws Exception {
		URI uri = URI.create("https://example.org/site/content.xml");
		primeStaleCacheEntry(uri, "cached-body");

		Response response = mock(Response.class);
		when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
		when(response.headers()).thenReturn(Collections.emptyMap());
		when(response.getURI()).thenReturn(uri);
		dispatchResponse(response);

		Logger logger = mock(Logger.class);
		File file = cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);

		assertEquals("cached-body", Files.readString(file.toPath()));
		assertEquals(CacheState.NOT_MODIFIED, cache.getLastCacheState(uri));
		verify(logger, atLeastOnce()).debug(contains("Up-to-date"));
		verify(logger, never()).info(contains("Downloading from"));
	}

	@Test
	void realDownload_200_logsDownloadingFrom() throws Exception {
		URI uri = URI.create("https://example.org/site/new-artifact.jar");

		Response response = mock(Response.class);
		when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(response.headers()).thenReturn(Collections.emptyMap());
		when(response.getURI()).thenReturn(uri);
		doAnswer(inv -> {
			OutputStream os = inv.getArgument(0);
			os.write("fresh-body".getBytes());
			return null;
		}).when(response).transferTo(any(OutputStream.class));
		dispatchResponse(response);

		Logger logger = mock(Logger.class);
		File file = cache.getCacheEntry(uri, logger).getCacheFile(transportFactory);

		assertEquals("fresh-body", Files.readString(file.toPath()));
		assertEquals(CacheState.DOWNLOADED, cache.getLastCacheState(uri));
		verify(logger, atLeastOnce()).info(contains("Downloading from"));
		verify(logger, never()).debug(contains("Fetched from cache"));
		verify(logger, never()).debug(contains("Up-to-date"));
	}

	/** Unknown URI reports UNKNOWN state (used by the transport to decide logging). */
	@Test
	void unknownUri_reportsUnknownState() {
		assertEquals(CacheState.UNKNOWN, cache.getLastCacheState(URI.create("https://example.org/never-seen")));
	}

	// Helpers

	private void primeFreshCacheEntry(URI uri, String body) throws Exception {
		File cacheFile = locateCacheFile(uri);
		cacheFile.getParentFile().mkdirs();
		Files.writeString(cacheFile.toPath(), body);

		Properties headers = new Properties();
		headers.setProperty("HTTP_RESPONSE_CODE", "200");
		headers.setProperty("FILE-LAST_UPDATED", String.valueOf(System.currentTimeMillis()));
		storeHeaders(cacheFile, headers);
	}

	private void primeStaleCacheEntry(URI uri, String body) throws Exception {
		File cacheFile = locateCacheFile(uri);
		cacheFile.getParentFile().mkdirs();
		Files.writeString(cacheFile.toPath(), body);

		Properties headers = new Properties();
		headers.setProperty("HTTP_RESPONSE_CODE", "200");
		// Force revalidation by making the "last updated" timestamp older than the
		// cache-control minimum period.
		headers.setProperty("FILE-LAST_UPDATED", "0");
		headers.setProperty(Headers.CACHE_CONTROL_HEADER, Headers.MUST_REVALIDATE_DIRECTIVE);
		storeHeaders(cacheFile, headers);
	}

	private void storeHeaders(File cacheFile, Properties headers) throws Exception {
		File headerFile = new File(cacheFile.getParent(), cacheFile.getName() + ".headers");
		try (OutputStream os = Files.newOutputStream(headerFile.toPath())) {
			headers.store(os, null);
		}
	}

	/**
	 * Replicates the path-mangling logic used by {@link SharedHttpCacheStorage} so
	 * the test can pre-seed a cache entry at the exact location the storage will
	 * look up.
	 */
	private File locateCacheFile(URI uri) {
		String cleanPath = uri.normalize().toASCIIString()
				.replace(':', '/').replace('?', '/').replace('&', '/').replace('*', '/').replaceAll("/+", "/");
		if (cleanPath.endsWith("/")) {
			cleanPath += ".idx";
		}
		return new File(cacheDir.toFile(), cleanPath);
	}

	@SuppressWarnings("unchecked")
	private void dispatchResponse(Response response) throws Exception {
		when(transport.get(any())).thenAnswer(inv -> {
			ResponseConsumer<Object> consumer = inv.getArgument(0);
			return consumer.handleResponse(response);
		});
		// ensure setHeader is a no-op on our mock
		doAnswer(inv -> null).when(transport).setHeader(anyString(), anyString());
	}
}
