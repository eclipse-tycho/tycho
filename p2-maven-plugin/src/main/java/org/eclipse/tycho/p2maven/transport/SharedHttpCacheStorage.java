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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class SharedHttpCacheStorage implements HttpCache {

	private static final int MAX_CACHE_LINES = Integer.getInteger("tycho.p2.transport.max-cache-lines", 1000);
	/**
	 * Assumes the following minimum caching period for remote files in minutes
	 */
	// TODO can we sync this with the time where maven updates snapshots?
	public static final long MIN_CACHE_PERIOD = Long.getLong("tycho.p2.transport.min-cache-minutes",
			TimeUnit.HOURS.toMinutes(1));
	private static final int MAX_IN_MEMORY = 1000;

	private final TransportCacheConfig transportCacheConfig;
	private final Map<File, CacheLine> entryCache;

	@Inject
	public SharedHttpCacheStorage(TransportCacheConfig transportCacheConfig) {
		this.transportCacheConfig = transportCacheConfig;
		this.entryCache = new LinkedHashMap<>(MAX_CACHE_LINES, 0.75f, true) {
			private static final long serialVersionUID = 1L;
			@Override
			protected boolean removeEldestEntry(final Map.Entry<File, CacheLine> eldest) {
				return (size() > MAX_IN_MEMORY);
			}
		};
	}

	/**
	 * Fetches the cache entry for this URI
	 * 
	 * @param uri
	 * @return
	 * @throws FileNotFoundException if the URI is know to be not found
	 */
	@Override
	public CacheEntry getCacheEntry(URI uri, Logger logger) throws FileNotFoundException {
		URI normalized = uri.normalize();
		CacheLine cacheLine = getCacheLine(normalized);
		if (!transportCacheConfig.isUpdate()) { // if not updates are forced ...
			int code = cacheLine.getResponseCode();
			if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new FileNotFoundException(normalized.toASCIIString());
			}
			if (code == HttpURLConnection.HTTP_MOVED_PERM) {
				return getCacheEntry(cacheLine.getRedirect(normalized), logger);
			}
		}
		return new CacheEntry() {

			@Override
			public long getLastModified(HttpTransportFactory transportFactory) throws IOException {
				if (transportCacheConfig.isOffline()) {
					return cacheLine.getLastModified(normalized, transportFactory,
							SharedHttpCacheStorage::mavenIsOffline, logger);
				}
				try {
					return cacheLine.fetchLastModified(normalized, transportFactory, logger);
				} catch (FileNotFoundException | AuthenticationFailedException e) {
					// for not found and failed authentication we can't do anything useful
					throw e;
				} catch (IOException e) {
					if (!transportCacheConfig.isUpdate() && cacheLine.getResponseCode() > 0) {
						// if we have something cached, use that ...
						logger.warn("Request to " + normalized + " failed, trying cache instead");
						return cacheLine.getLastModified(normalized, transportFactory, nil -> e, logger);
					}
					throw e;
				}
			}

			@Override
			public File getCacheFile(HttpTransportFactory transportFactory) throws IOException {
				if (transportCacheConfig.isOffline()) {
					return cacheLine.getFile(normalized, transportFactory, SharedHttpCacheStorage::mavenIsOffline,
							logger);
				}
				try {
					return cacheLine.fetchFile(normalized, transportFactory, logger);
				} catch (FileNotFoundException | AuthenticationFailedException e) {
					// for not found and failed authentication we can't do anything useful
					throw e;
				} catch (IOException e) {
					if (!transportCacheConfig.isUpdate() && cacheLine.getResponseCode() > 0) {
						// if we have something cached, use that ...
						logger.warn("Request to " + normalized + " failed, trying cache instead");
						return cacheLine.getFile(normalized, transportFactory, nil -> e, logger);
					}
					throw e;
				}
			}

		};
	}

	private synchronized CacheLine getCacheLine(URI uri) {
		String cleanPath = uri.normalize().toASCIIString().replace(':', '/').replace('?', '/').replace('&', '/')
				.replace('*', '/').replaceAll("/+", "/");
		if (cleanPath.endsWith("/")) {
			// simulate accessing this as a folder...
			// this can happen in case of a redirect even though its quite clumsy
			cleanPath += ".idx";
		}
		File file = new File(transportCacheConfig.getCacheLocation(), cleanPath);
		File location;
		try {
			location = file.getCanonicalFile();
		} catch (IOException e) {
			location = file.getAbsoluteFile();
		}
		return entryCache.computeIfAbsent(location, CacheLine::new);

	}

	private final class CacheLine {

		private static final String RESPONSE_CODE = "HTTP_RESPONSE_CODE";
		private static final String LAST_UPDATED = "FILE-LAST_UPDATED";
		private static final String STATUS_LINE = "HTTP_STATUS_LINE";
		private final File file;
		private final File headerFile;
		private Properties header;
		private final DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

		public CacheLine(File file) {
			this.file = file;
			this.headerFile = new File(file.getParent(), file.getName() + ".headers");
			httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		public synchronized long fetchLastModified(URI uri, HttpTransportFactory transportFactory, Logger logger)
				throws IOException {
			// TODO its very likely that the file is downloaded here if it has changed... so
			// probably just download it right now?
			HttpTransport transport = transportFactory.createTransport(uri);

			try (Headers response = transport.head()) {
				int code = response.statusCode();
				if (isAuthFailure(code)) {
					throw new AuthenticationFailedException(); // FIXME why is there no constructor to give a cause?
				}
				if (isNotFound(code)) {
					updateHeader(response, code);
					throw new FileNotFoundException(uri.toString());
				}
				if (isRedirected(code)) {
					updateHeader(response, code);
					return SharedHttpCacheStorage.this.getCacheEntry(uri, logger).getLastModified(transportFactory);
				}
				return response.getLastModified();
			}
		}

		public synchronized long getLastModified(URI uri, HttpTransportFactory transportFactory,
				Function<URI, IOException> notAviableExceptionSupplier, Logger logger) throws IOException {
			int code = getResponseCode();
			if (code > 0) {
				if (isAuthFailure(code)) {
					throw new AuthenticationFailedException(); // FIXME why is there no constructor to give a cause?
				}
				if (isNotFound(code)) {
					throw new FileNotFoundException(uri.toString());
				}
				if (isRedirected(code)) {
					return SharedHttpCacheStorage.this.getCacheEntry(uri, logger).getLastModified(transportFactory);
				}
				Properties offlineHeader = getHeader();
				Date lastModified = pareHttpDate(offlineHeader.getProperty(Headers.LAST_MODIFIED_HEADER.toLowerCase()));
				if (lastModified != null) {
					return lastModified.getTime();
				}
				return -1;
			} else {
				throw notAviableExceptionSupplier.apply(uri);
			}
		}

		public synchronized File fetchFile(URI uri, HttpTransportFactory transportFactory, Logger logger)
				throws IOException {
			boolean exits = file.isFile();
			if (exits && !mustValidate()) {
				return file;
			}
			HttpTransport transport = transportFactory.createTransport(uri);
			Properties lastHeader = getHeader();
			if (exits) {
				if (lastHeader.containsKey(Headers.ETAG_HEADER.toLowerCase())) {
					transport.setHeader("If-None-Match", lastHeader.getProperty(Headers.ETAG_HEADER.toLowerCase()));
				}
				if (lastHeader.contains(Headers.LAST_MODIFIED_HEADER.toLowerCase())) {
					transport.setHeader("If-Modified-Since",
							lastHeader.getProperty(Headers.LAST_MODIFIED_HEADER.toLowerCase()));
				}
			}
			transport.setHeader(Headers.HEADER_ACCEPT_ENCODING, Headers.ENCODING_GZIP);
			return transport.get(response -> {
				File tempFile;
				int code = response.statusCode();
				if (exits && code == HttpURLConnection.HTTP_NOT_MODIFIED) {
					updateHeader(response, getResponseCode());
					return file;
				}
				if (isAuthFailure(code)) {
					throw new AuthenticationFailedException(); // FIXME why is there no constructor to give a cause?
				}
				updateHeader(response, code);
				if (isRedirected(code)) {
					File cachedFile = SharedHttpCacheStorage.this.getCacheEntry(getRedirect(uri), logger)
							.getCacheFile(transportFactory);
					// https://github.com/eclipse-tycho/tycho/issues/2938
					// Redirect may change extension. P2's SimpleMetadataRepositoryFactory relies on
					// accurate file extension to be cached.
					// Copying file to accommodate original request and its file extension.
					// Once https://github.com/eclipse-equinox/p2/issues/355 is fixed, cachedFile
					// may be returned directly without copying.
					response.close(); // early close before doing unrelated file I/O
					FileUtils.copyFile(cachedFile, file);
					return file;
				}
				if (exits) {
					FileUtils.forceDelete(file);
				}
				response.checkResponseCode();
				tempFile = File.createTempFile("download", ".tmp", file.getParentFile());
				try (FileOutputStream os = new FileOutputStream(tempFile)) {
					response.transferTo(os);
				} catch (IOException e) {
					tempFile.delete();
					throw e;
				}
				response.close(); // early close before doing file I/O
				FileUtils.moveFile(tempFile, file);
				return file;
			});

		}

		public synchronized File getFile(URI uri, HttpTransportFactory transportFactory,
				Function<URI, IOException> notAviableExceptionSupplier, Logger logger) throws IOException {
			int code = getResponseCode();
			if (code > 0) {
				if (isAuthFailure(code)) {
					throw new AuthenticationFailedException(); // FIXME why is there no constructor to give a cause?
				}
				if (isNotFound(code)) {
					throw new FileNotFoundException(uri.toString());
				}
				if (isRedirected(code)) {
					return SharedHttpCacheStorage.this.getCacheEntry(getRedirect(uri), logger)
							.getCacheFile(transportFactory);
				}
				if (file.isFile()) {
					return file;
				}
			}
			throw notAviableExceptionSupplier.apply(uri);
		}

		private boolean mustValidate() {
			if (transportCacheConfig.isUpdate()) {
				// user enforced validation
				return true;
			}
			String[] cacheControls = getCacheControl();
			for (String directive : cacheControls) {
				if (Headers.MUST_REVALIDATE_DIRECTIVE.equals(directive)) {
					// server enforced validation
					return true;
				}
			}
			Properties properties = getHeader();
			long lastUpdated = parseLong(properties.getProperty(LAST_UPDATED));
			if (lastUpdated + TimeUnit.MINUTES.toMillis(MIN_CACHE_PERIOD) > System.currentTimeMillis()) {
				return false;
			}
			// Cache-Control header with "max-age" directive takes precedence over Expires
			// Header.
			for (String directive : cacheControls) {
				if (directive.toLowerCase().startsWith(Headers.MAX_AGE_DIRECTIVE)) {
					long maxAge = parseLong(directive.substring(Headers.MAX_AGE_DIRECTIVE.length() + 1));
					if (maxAge <= 0) {
						return true;
					}
					return (lastUpdated + TimeUnit.SECONDS.toMillis(maxAge)) < System.currentTimeMillis();
				}
			}
			Date expiresDate = pareHttpDate(properties.getProperty(Headers.EXPIRES_HEADER.toLowerCase()));
			if (expiresDate != null) {
				return expiresDate.after(new Date());
			}
			return true;
		}

		protected long parseLong(String value) {
			if (value != null) {
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException e) {
					// ignore...
				}
			}
			return 0;
		}

		private String[] getCacheControl() {
			String property = getHeader().getProperty(Headers.CACHE_CONTROL_HEADER);
			if (property != null) {
				return property.split(",\\s*");
			}
			return new String[0];
		}

		protected boolean isAuthFailure(int code) {
			return code == HttpURLConnection.HTTP_PROXY_AUTH || code == HttpURLConnection.HTTP_UNAUTHORIZED;
		}

		protected void updateHeader(Headers response, int code) throws IOException, FileNotFoundException {
			header = new Properties();
			header.setProperty(RESPONSE_CODE, String.valueOf(code));
			header.setProperty(LAST_UPDATED, String.valueOf(System.currentTimeMillis()));
			Map<String, List<String>> headerFields = response.headers();
			for (var entry : headerFields.entrySet()) {
				String key = entry.getKey();
				if (key == null) {
					key = STATUS_LINE;
				}
				key = key.toLowerCase();
				if (MavenAuthenticator.AUTHORIZATION_HEADER.equalsIgnoreCase(key)
						|| MavenAuthenticator.PROXY_AUTHORIZATION_HEADER.equalsIgnoreCase(key)) {
					// Don't store sensitive information here...
					continue;
				}
				if (key.toLowerCase().startsWith("x-")) {
					// don't store non default header...
					continue;
				}
				if (Headers.HEADER_CONTENT_ENCODING.equals(key)) {
					// we already decode the content before...
					continue;
				}
				List<String> value = entry.getValue();
				if (value.size() == 1) {
					header.put(key, value.get(0));
				} else {
					header.put(key, value.stream().collect(Collectors.joining(",")));
				}
			}
			FileUtils.forceMkdir(file.getParentFile());
			try (FileOutputStream out = new FileOutputStream(headerFile)) {
				// we store the header here, this might be a 404 response or (permanent)
				// redirect we probably need to work with later on
				header.store(out, null);
			}
		}

		private synchronized Date pareHttpDate(String input) {
			if (input != null) {
				try {
					return httpDateFormat.parse(input);
				} catch (ParseException e) {
					// can't use it then..
				}
			}
			return null;
		}

		public int getResponseCode() {
			return Integer.parseInt(getHeader().getProperty(RESPONSE_CODE, "-1"));
		}

		public URI getRedirect(URI base) throws FileNotFoundException {
			String location = getHeader().getProperty("location");
			if (location == null) {
				throw new FileNotFoundException(base.toASCIIString());
			}
			return base.resolve(location);
		}

		public Properties getHeader() {
			if (header == null) {
				header = new Properties();
				if (headerFile.isFile()) {
					try {
						header.load(new FileInputStream(headerFile));
					} catch (IOException e) {
						// can't use the headers then...
					}
				}
			}
			return header;
		}
	}

	private static boolean isRedirected(int code) {
		return code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP;
	}

	private static boolean isNotFound(int code) {
		return code == HttpURLConnection.HTTP_NOT_FOUND;
	}

	private static IOException mavenIsOffline(URI uri) {
		return new IOException("maven is currently in offline mode requested URL " + uri + " does not exist locally!");
	}

}
