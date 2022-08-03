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
package org.eclipse.tycho.p2.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
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

import org.apache.commons.io.FileUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.ecf.provider.filetransfer.util.ProxySetupHelper;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.tycho.core.resolver.shared.MavenRepositorySettings.Credentials;
import org.eclipse.tycho.core.shared.MavenLogger;

public class SharedHttpCacheStorage {

    /**
     * Assumes the following minimum caching period for remote files in minutes
     */
    //TODO can we sync this with the time where maven updates snapshots?
    public static final long MIN_CACHE_PERIOD = Long.getLong("tycho.p2.transport.min-cache-minutes",
            TimeUnit.HOURS.toMinutes(1));
    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    private static final String AUTHORIZATION_HEADER = "Authorization";
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
        entryCache = new LinkedHashMap<File, CacheLine>(100, 0.75f, true) {

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
    public CacheEntry getCacheEntry(URI uri, MavenLogger logger) throws FileNotFoundException {
        CacheLine cacheLine = getCacheLine(uri);
        if (!cacheConfig.update) { //if not updates are forced ...
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
            public long getLastModified(IProxyService proxyService, Function<URI, Credentials> credentialsProvider)
                    throws IOException {
                if (cacheConfig.offline) {
                    return cacheLine.getLastModified(uri, proxyService, credentialsProvider,
                            SharedHttpCacheStorage::mavenIsOffline, logger);
                }
                try {
                    return cacheLine.fetchLastModified(uri, proxyService, credentialsProvider, logger);
                } catch (FileNotFoundException | AuthenticationFailedException e) {
                    //for not found and failed authentication we can't do anything useful
                    throw e;
                } catch (IOException e) {
                    if (!cacheConfig.update && cacheLine.getResponseCode() > 0) {
                        //if we have something cached, use that ...
                        logger.warn("Request to " + uri + " failed, trying cache instead...");
                        return cacheLine.getLastModified(uri, proxyService, credentialsProvider, nil -> e, logger);
                    }
                    throw e;
                }
            }

            @Override
            public File getCacheFile(IProxyService proxyService, Function<URI, Credentials> credentialsProvider)
                    throws IOException {
                if (cacheConfig.offline) {
                    return cacheLine.getFile(uri, proxyService, credentialsProvider,
                            SharedHttpCacheStorage::mavenIsOffline, logger);
                }
                try {
                    return cacheLine.fetchFile(uri, proxyService, credentialsProvider, logger);
                } catch (FileNotFoundException | AuthenticationFailedException e) {
                    //for not found and failed authentication we can't do anything useful
                    throw e;
                } catch (IOException e) {
                    if (!cacheConfig.update && cacheLine.getResponseCode() > 0) {
                        //if we have something cached, use that ...
                        logger.warn("Request to " + uri + " failed, trying cache instead...");
                        return cacheLine.getFile(uri, proxyService, credentialsProvider, nil -> e, logger);
                    }
                    throw e;
                }
            }

        };
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

        public synchronized long fetchLastModified(URI uri, IProxyService proxyService,
                Function<URI, Credentials> credentialsProvider, MavenLogger logger) throws IOException {
            //TODO its very likely that the file is downloaded here if it has changed... so probably just download it right now?
            RepositoryAuthenticator authenticator = new RepositoryAuthenticator(getProxyData(proxyService, uri),
                    credentialsProvider.apply(uri));
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(authenticator.getProxy());
            connection.setAuthenticator(authenticator);
            connection.setRequestMethod("HEAD");
            authenticator.preemtiveAuth(connection);
            connection.connect();
            try {
                int code = connection.getResponseCode();
                if (isAuthFailure(code)) {
                    throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
                }
                if (isNotFound(code)) {
                    updateHeader(connection, code);
                    throw new FileNotFoundException(uri.toString());
                }
                if (isRedirected(code)) {
                    updateHeader(connection, code);
                    return SharedHttpCacheStorage.this.getCacheEntry(uri, logger).getLastModified(proxyService,
                            credentialsProvider);
                }
                return connection.getLastModified();
            } finally {
                closeConnection(connection);
            }
        }

        public synchronized long getLastModified(URI uri, IProxyService proxyService,
                Function<URI, Credentials> credentialsProvider, Function<URI, IOException> notAviableExceptionSupplier,
                MavenLogger logger) throws IOException {
            int code = getResponseCode();
            if (code > 0) {
                if (isAuthFailure(code)) {
                    throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
                }
                if (isNotFound(code)) {
                    throw new FileNotFoundException(uri.toString());
                }
                if (isRedirected(code)) {
                    return SharedHttpCacheStorage.this.getCacheEntry(uri, logger).getLastModified(proxyService,
                            credentialsProvider);
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

        public synchronized File fetchFile(URI uri, IProxyService proxyService,
                Function<URI, Credentials> credentialsProvider, MavenLogger logger) throws IOException {
            boolean exits = file.isFile();
            if (exits && !mustValidate()) {
                return file;
            }
            RepositoryAuthenticator authenticator = new RepositoryAuthenticator(getProxyData(proxyService, uri),
                    credentialsProvider.apply(uri));
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(authenticator.getProxy());
            connection.setAuthenticator(authenticator);
            authenticator.preemtiveAuth(connection);
            Properties lastHeader = getHeader();
            if (exits) {
                if (lastHeader.containsKey(ETAG_HEADER.toLowerCase())) {
                    connection.setRequestProperty("If-None-Match", lastHeader.getProperty(ETAG_HEADER.toLowerCase()));
                }
                if (lastHeader.contains(LAST_MODIFIED_HEADER.toLowerCase())) {
                    connection.setRequestProperty("If-Modified-Since",
                            lastHeader.getProperty(LAST_MODIFIED_HEADER.toLowerCase()));
                }
            }
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            int code = connection.getResponseCode();
            if (exits && code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                updateHeader(connection, getResponseCode());
                return file;
            }
            if (isAuthFailure(code)) {
                throw new AuthenticationFailedException(); //FIXME why is there no constructor to give a cause?
            }
            updateHeader(connection, code);
            if (isRedirected(code)) {
                closeConnection(connection);
                return SharedHttpCacheStorage.this.getCacheEntry(getRedirect(uri), logger).getCacheFile(proxyService,
                        credentialsProvider);
            }
            if (exits) {
                FileUtils.forceDelete(file);
            }
            File tempFile = File.createTempFile("download", ".tmp", file.getParentFile());
            try (InputStream inputStream = connection.getInputStream();
                    FileOutputStream os = new FileOutputStream(tempFile)) {
                inputStream.transferTo(os);
            } catch (IOException e) {
                tempFile.delete();
                throw e;
            }
            FileUtils.moveFile(tempFile, file);
            return file;
        }

        public synchronized File getFile(URI uri, IProxyService proxyService,
                Function<URI, Credentials> credentialsProvider, Function<URI, IOException> notAviableExceptionSupplier,
                MavenLogger logger) throws IOException {
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
                            .getCacheFile(proxyService, credentialsProvider);
                }
                if (file.isFile()) {
                    return file;
                }
            }
            throw notAviableExceptionSupplier.apply(uri);
        }

        private boolean mustValidate() {
            if (cacheConfig.update) {
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

        protected void updateHeader(HttpURLConnection connection, int code) throws IOException, FileNotFoundException {
            header = new Properties();
            header.setProperty(RESPONSE_CODE, String.valueOf(code));
            header.setProperty(LAST_UPDATED, String.valueOf(System.currentTimeMillis()));
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (var entry : headerFields.entrySet()) {
                String key = entry.getKey();
                if (key == null) {
                    key = STATUS_LINE;
                }
                key = key.toLowerCase();
                if (AUTHORIZATION_HEADER.equalsIgnoreCase(key) || PROXY_AUTHORIZATION_HEADER.equalsIgnoreCase(key)) {
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

        private void closeConnection(HttpURLConnection connection) {
            try {
                connection.getInputStream().close();
            } catch (IOException e) {
                //we just wan't to signal that we are done with this connection...
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
                        //can't use the headers then...
                    }
                }
            }
            return header;
        }
    }

    private static IProxyData getProxyData(IProxyService proxyService, URI uri) throws IOException {
        if (proxyService != null) {
            IProxyData[] selected = proxyService.select(uri);
            IProxyData proxyData = ProxySetupHelper.selectProxyFromProxies(uri.getScheme(), selected);
            if (proxyData != null) {
                return proxyData;
            }
        }
        return null;
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
        public CacheConfig(File location, boolean offline, boolean update) {
            this.location = location;
            this.offline = offline;
            this.update = update;
        }

        private final File location;
        private final boolean offline;
        private final boolean update;

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

    private static final class RepositoryAuthenticator extends Authenticator {

        private IProxyData proxyData;
        private Credentials credentials;

        public RepositoryAuthenticator(IProxyData proxyData, Credentials credentials) {
            this.proxyData = proxyData;
            this.credentials = credentials;
        }

        public void preemtiveAuth(HttpURLConnection connection) {
            // as everything is known and we can't ask the user anyways, preemtive auth is a good choice here to prevent successive requests
            addAuthHeader(connection, getPasswordAuthentication(RequestorType.PROXY), PROXY_AUTHORIZATION_HEADER);
            addAuthHeader(connection, getPasswordAuthentication(RequestorType.SERVER), AUTHORIZATION_HEADER);
        }

        private void addAuthHeader(HttpURLConnection connection, PasswordAuthentication authentication, String header) {
            if (authentication == null) {
                return;
            }
            String encoding = Base64.getEncoder().encodeToString(
                    (authentication.getUserName() + ":" + new String(authentication.getPassword())).getBytes());
            connection.setRequestProperty(header, "Basic " + encoding);

        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return getPasswordAuthentication(getRequestorType());
        }

        protected PasswordAuthentication getPasswordAuthentication(RequestorType type) {
            if (type == RequestorType.PROXY) {
                if (proxyData != null) {
                    String userId = proxyData.getUserId();
                    if (userId != null) {
                        String password = proxyData.getPassword();
                        return new PasswordAuthentication(userId,
                                password == null ? new char[0] : password.toCharArray());
                    }
                }
            } else if (type == RequestorType.SERVER) {
                if (credentials != null) {
                    String userName = credentials.getUserName();
                    if (userName != null) {
                        String password = credentials.getPassword();
                        return new PasswordAuthentication(userName,
                                password == null ? new char[0] : password.toCharArray());
                    }
                }
            }
            return null;
        }

        public Proxy getProxy() {
            if (proxyData == null) {
                return Proxy.NO_PROXY;
            }
            return new Proxy(convertType(proxyData), convertAddress(proxyData));
        }

        private static SocketAddress convertAddress(IProxyData data) {
            return new InetSocketAddress(data.getHost(), data.getPort());
        }

        private static Type convertType(IProxyData data) {
            switch (data.getType()) {
            case IProxyData.HTTPS_PROXY_TYPE:
            case IProxyData.HTTP_PROXY_TYPE:
                return Type.HTTP;
            case IProxyData.SOCKS_PROXY_TYPE:
                return Type.SOCKS;
            default:
                return Type.DIRECT;
            }
        }
    }

}
