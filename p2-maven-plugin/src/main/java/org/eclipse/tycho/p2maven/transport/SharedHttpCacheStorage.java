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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;

@Component(role = HttpCache.class)
public class SharedHttpCacheStorage implements HttpCache {

	private static final int MAX_CACHE_LINES = Integer.getInteger("tycho.p2.transport.max-cache-lines", 1000);
	/**
     * Assumes the following minimum caching period for remote files in minutes
     */
    //TODO can we sync this with the time where maven updates snapshots?
    public static final long MIN_CACHE_PERIOD = Long.getLong("tycho.p2.transport.min-cache-minutes",
            TimeUnit.HOURS.toMinutes(1));
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";
    private static final String EXPIRES_HEADER = "Expires";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String MAX_AGE_DIRECTIVE = "max-age";
    private static final String MUST_REVALIDATE_DIRECTIVE = "must-revalidate";

    private static final String ETAG_HEADER = "ETag";

    private static final int MAX_IN_MEMORY = 1000;

	@Requirement
	TransportCacheConfig cacheConfig;

    private final Map<File, CacheLine> entryCache;


	public SharedHttpCacheStorage() {

		entryCache = new LinkedHashMap<>(MAX_CACHE_LINES, 0.75f, true) {

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
     * @throws FileNotFoundException
     *             if the URI is know to be not found
     */
	@Override
	public CacheEntry getCacheEntry(URI uri, Logger logger) throws FileNotFoundException {
        CacheLine cacheLine = getCacheLine(uri);
		if (!cacheConfig.isUpdate()) { // if not updates are forced ...
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
			public long getLastModified(HttpTransportFactory transportFactory)
                    throws IOException {
				if (cacheConfig.isOffline()) {
                    return cacheLine.getLastModified(uri, transportFactory,
                            SharedHttpCacheStorage::mavenIsOffline, logger);
                }
                try {
					return cacheLine.fetchLastModified(uri, transportFactory, logger);
                } catch (FileNotFoundException | AuthenticationFailedException e) {
                    //for not found and failed authentication we can't do anything useful
                    throw e;
                } catch (IOException e) {
					if (!cacheConfig.isUpdate() && cacheLine.getResponseCode() > 0) {
                        //if we have something cached, use that ...
                        logger.warn("Request to " + uri + " failed, trying cache instead");
						return cacheLine.getLastModified(uri, transportFactory, nil -> e, logger);
                    }
                    throw e;
                }
            }

            @Override
			public File getCacheFile(HttpTransportFactory transportFactory)
                    throws IOException {
				if (cacheConfig.isOffline()) {
					return cacheLine.getFile(uri, transportFactory,
                            SharedHttpCacheStorage::mavenIsOffline, logger);
                }
                try {
					return cacheLine.fetchFile(uri, transportFactory, logger);
                } catch (FileNotFoundException | AuthenticationFailedException e) {
                    //for not found and failed authentication we can't do anything useful
                    throw e;
                } catch (IOException e) {
					if (!cacheConfig.isUpdate() && cacheLine.getResponseCode() > 0) {
                        //if we have something cached, use that ...
                        logger.warn("Request to " + uri + " failed, trying cache instead");
						return cacheLine.getFile(uri, transportFactory, nil -> e, logger);
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
		File file = new File(cacheConfig.getCacheLocation(), cleanPath);
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
            //TODO its very likely that the file is downloaded here if it has changed... so probably just download it right now?
			HttpTransport transport = transportFactory.createTransport(uri);

			try (Response<Void> response = transport.head()) {
				int code = response.statusCode();
                if (isAuthFailure(code)) {
                    throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
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
				Function<URI, IOException> notAviableExceptionSupplier,
				Logger logger) throws IOException {
            int code = getResponseCode();
            if (code > 0) {
                if (isAuthFailure(code)) {
                    throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
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
			HttpTransport transport = transportFactory.createTransport(uri);
            Properties lastHeader = getHeader();
            if (exits) {
                if (lastHeader.containsKey(ETAG_HEADER.toLowerCase())) {
					transport.setHeader("If-None-Match", lastHeader.getProperty(ETAG_HEADER.toLowerCase()));
                }
                if (lastHeader.contains(LAST_MODIFIED_HEADER.toLowerCase())) {
					transport.setHeader("If-Modified-Since",
                            lastHeader.getProperty(LAST_MODIFIED_HEADER.toLowerCase()));
                }
            }
			try (Response<InputStream> response = transport.get()) {
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
				response.checkResponseCode();
				File tempFile = File.createTempFile("download", ".tmp", file.getParentFile());
				try (InputStream inputStream = response.body(); FileOutputStream os = new FileOutputStream(tempFile)) {
					inputStream.transferTo(os);
				} catch (IOException e) {
					tempFile.delete();
					throw e;
				}
				FileUtils.moveFile(tempFile, file);
			}
			return file;
        }

		public synchronized File getFile(URI uri, HttpTransportFactory transportFactory,
				Function<URI, IOException> notAviableExceptionSupplier,
				Logger logger) throws IOException {
            int code = getResponseCode();
            if (code > 0) {
                if (isAuthFailure(code)) {
                    throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
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
			if (cacheConfig.isUpdate()) {
                //user enforced validation
                return true;
            }
            String[] cacheControls = getCacheControl();
            for (String directive : cacheControls) {
                if (MUST_REVALIDATE_DIRECTIVE.equals(directive)) {
                    //server enforced validation
                    return true;
                }
            }
            Properties properties = getHeader();
            long lastUpdated = parseLong(properties.getProperty(LAST_UPDATED));
            if (lastUpdated + TimeUnit.MINUTES.toMillis(MIN_CACHE_PERIOD) > System.currentTimeMillis()) {
                return false;
            }
            //Cache-Control header with "max-age" directive takes precedence over Expires Header.
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
                    //ignore...
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

		protected void updateHeader(Response<?> response, int code)
				throws IOException, FileNotFoundException {
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
                    //Don't store sensitive information here...
                    continue;
                }
                if (key.toLowerCase().startsWith("x-")) {
                    //don't store non default header...
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
                //we store the header here, this might be a 404 response or (permanent) redirect we probably need to work with later on
                header.store(out, null);
            }
        }

        private synchronized Date pareHttpDate(String input) {
            if (input != null) {
                try {
                    return httpDateFormat.parse(input);
                } catch (ParseException e) {
                    //can't use it then..
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
                        //can't use the headers then...
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
