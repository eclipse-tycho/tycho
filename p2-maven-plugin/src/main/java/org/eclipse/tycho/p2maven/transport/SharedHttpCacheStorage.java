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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;

public class SharedHttpCacheStorage {

	private static final String GZIP_ENCODING = "gzip";

	private static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";

	private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

	// see https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
	// per RFC there are three different formats:
	private static final List<ThreadLocal<DateFormat>> DATE_PATTERNS = List.of(//
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")), // RFC 1123
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz")), // RFC 1036
			ThreadLocal.withInitial(() -> new SimpleDateFormat("EEE MMMd HH:mm:ss yyyy")) // ANSI C's asctime() format
	);
	/**
	 * Assumes the following minimum caching period for remote files in minutes
	 */
	// TODO can we sync this with the time where maven updates snapshots?
	public static final long MIN_CACHE_PERIOD = Long.getLong("tycho.p2.transport.min-cache-minutes",
			TimeUnit.HOURS.toMinutes(1));
	private static final String LAST_MODIFIED_HEADER = "Last-Modified";
	private static final String EXPIRES_HEADER = "Expires";
	private static final String CACHE_CONTROL_HEADER = "Cache-Control";
	private static final String MAX_AGE_DIRECTIVE = "max-age";
	private static final String MUST_REVALIDATE_DIRECTIVE = "must-revalidate";

	private static final String ETAG_HEADER = "ETag";

	private static final Map<CacheConfig, SharedHttpCacheStorage> storageMap = new HashMap<>();

	private static final int MAX_IN_MEMORY = 1000;

	private final Map<File, CacheLine> entryCache;

	private CacheConfig cacheConfig;

	private SharedHttpCacheStorage(CacheConfig cacheConfig) {

		this.cacheConfig = cacheConfig;
		entryCache = new LinkedHashMap<>(100, 0.75f, true) {

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
	public CacheEntry getCacheEntry(URI uri, Logger logger) throws FileNotFoundException {
		CacheLine cacheLine = getCacheLine(uri);
		if (!cacheConfig.update) { // if not updates are forced ...
			int code = cacheLine.getResponseCode();
			if (code == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new FileNotFoundException(uri.toASCIIString());
			}
			if (code == HttpURLConnection.HTTP_MOVED_PERM) {
				return getCacheEntry(cacheLine.getRedirect(uri), logger);
			}
		}
		return new CacheEntry() {

			@Override
			public long getLastModified(HttpTransportFactory transportFactory) throws IOException {
				if (cacheConfig.offline) {
					return cacheLine.getLastModified(uri, SharedHttpCacheStorage::mavenIsOffline, transportFactory,
							logger);
				}
				try {
					return cacheLine.fetchLastModified(uri, transportFactory, logger);
				} catch (FileNotFoundException | AuthenticationFailedException e) {
					// for not found and failed authentication we can't do anything useful
					throw e;
				} catch (IOException e) {
					if (!cacheConfig.update && cacheLine.getResponseCode() > 0) {
						// if we have something cached, use that ...
						logger.warn("Request to " + uri + " failed, trying cache instead...");
						return cacheLine.getLastModified(uri, nil -> e, transportFactory, logger);
					}
					throw e;
				}
			}

			@Override
			public File getCacheFile(HttpTransportFactory transportFactory) throws IOException {
				if (cacheConfig.offline) {
					return cacheLine.getFile(uri, SharedHttpCacheStorage::mavenIsOffline, transportFactory, logger);
				}
				try {
					return cacheLine.fetchFile(uri, transportFactory, logger);
				} catch (FileNotFoundException | AuthenticationFailedException e) {
					// for not found and failed authentication we can't do anything useful
					throw e;
				} catch (IOException e) {
					if (!cacheConfig.update && cacheLine.getResponseCode() > 0) {
						// if we have something cached, use that ...
						logger.warn("Request to " + uri + " failed, trying cache instead...");
						return cacheLine.getFile(uri, nil -> e, transportFactory, logger);
					}
					throw e;
				}
			}

		};
	}

	public static long getLastModifiedTimeFromHeader(String lastModifiedHeader) throws IOException {
		if (lastModifiedHeader == null)
			return 0L;
		// first check if there are any quotes around and remove them
		if (lastModifiedHeader.length() > 1 && lastModifiedHeader.startsWith("'") && lastModifiedHeader.endsWith("'")) {
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
		return -1;
	}

	private synchronized CacheLine getCacheLine(URI uri) {
		File file = new File(cacheConfig.location, uri.normalize().toASCIIString().replace(':', '/').replace('?', '/')
				.replace('&', '/').replaceAll("/+", "/"));
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
			// probably just download it right now and put it in the cache?
			HttpTransport transport = transportFactory.createTransport(uri);

			try (Response<Void> response = transport.head()) {
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
				return getLastModifiedTimeFromHeader(response.getHeader(LAST_MODIFIED_HEADER));
			}
		}

		public synchronized long getLastModified(URI uri, Function<URI, IOException> notAviableExceptionSupplier,
				HttpTransportFactory transportFactory, Logger logger) throws IOException {
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
				Date lastModified = pareHttpDate(offlineHeader.getProperty(LAST_MODIFIED_HEADER.toLowerCase()));
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
			HttpTransport httpTransport = transportFactory.createTransport(uri);
			httpTransport.setHeader(ACCEPT_ENCODING_HEADER, GZIP_ENCODING);
			Properties lastHeader = getHeader();
			if (exits) {
				if (lastHeader.containsKey(ETAG_HEADER.toLowerCase())) {
					httpTransport.setHeader("If-None-Match", lastHeader.getProperty(ETAG_HEADER.toLowerCase()));
				}
				if (lastHeader.contains(LAST_MODIFIED_HEADER.toLowerCase())) {
					httpTransport.setHeader("If-Modified-Since",
							lastHeader.getProperty(LAST_MODIFIED_HEADER.toLowerCase()));
				}
			}
			try (Response<InputStream> response = httpTransport.get()) {
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
					return SharedHttpCacheStorage.this.getCacheEntry(getRedirect(uri), logger)
							.getCacheFile(transportFactory);
				}
				if (exits) {
					FileUtils.forceDelete(file);
				}
				File tempFile = File.createTempFile("download", ".tmp", file.getParentFile());
				try (InputStream inputStream = response.body(); FileOutputStream os = new FileOutputStream(tempFile)) {
					if (GZIP_ENCODING.equalsIgnoreCase(response.getHeader(CONTENT_ENCODING_HEADER))) {
						new GZIPInputStream(inputStream).transferTo(os);
					} else {
						inputStream.transferTo(os);
					}
				} catch (IOException e) {
					tempFile.delete();
					throw e;
				}
				FileUtils.moveFile(tempFile, file);
			}

			return file;
		}

		public synchronized File getFile(URI uri, Function<URI, IOException> notAviableExceptionSupplier,
				HttpTransportFactory transportFactory, Logger logger) throws IOException {
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
			if (cacheConfig.update) {
				// user enforced validation
				return true;
			}
			String[] cacheControls = getCacheControl();
			for (String directive : cacheControls) {
				if (MUST_REVALIDATE_DIRECTIVE.equals(directive)) {
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
				if (directive.toLowerCase().startsWith(MAX_AGE_DIRECTIVE)) {
					long maxAge = parseLong(directive.substring(MAX_AGE_DIRECTIVE.length() + 1));
					if (maxAge <= 0) {
						return true;
					}
					return (lastUpdated + TimeUnit.SECONDS.toMillis(maxAge)) < System.currentTimeMillis();
				}
			}
			Date expiresDate = pareHttpDate(properties.getProperty(EXPIRES_HEADER.toLowerCase()));
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
			String property = getHeader().getProperty(CACHE_CONTROL_HEADER);
			if (property != null) {
				return property.split(",\\s*");
			}
			return new String[0];
		}

		protected boolean isAuthFailure(int code) {
			return code == HttpURLConnection.HTTP_PROXY_AUTH || code == HttpURLConnection.HTTP_UNAUTHORIZED;
		}

		protected void updateHeader(Response<?> response, int code) throws IOException, FileNotFoundException {
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

		private void closeConnection(HttpResponse<InputStream> response) {
			try (InputStream stream = response.body()) {
				stream.transferTo(OutputStream.nullOutputStream());
			} catch (IOException e) {
				// we just wan't to signal that we are done with this connection...
			}
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

	public static SharedHttpCacheStorage getStorage(File location, boolean offline, boolean update) {
		return storageMap.computeIfAbsent(new CacheConfig(location, offline, update), SharedHttpCacheStorage::new);
	}

	private static IOException mavenIsOffline(URI uri) {
		return new IOException("maven is currently in offline mode requested URL " + uri + " does not exist locally!");
	}

	private static final class CacheConfig {

		private final File location;
		private final boolean offline;
		private final boolean update;

		public CacheConfig(File location, boolean offline, boolean update) {
			this.location = location;
			this.offline = offline;
			this.update = update;
		}

		@Override
		public int hashCode() {
			return Objects.hash(location, offline, update);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheConfig other = (CacheConfig) obj;
			return Objects.equals(location, other.location) && offline == other.offline && update == other.update;
		}
	}

}
