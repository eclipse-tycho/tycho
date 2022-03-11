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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheEntrySerializer;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;
import org.apache.http.impl.client.cache.CacheConfig;

@Contract(threading = ThreadingBehavior.SAFE)
public class SharedHttpCacheStorage implements HttpCacheStorage {

    private File cacheLocation;
    private HttpCacheEntrySerializer cacheEntrySerializer;

    private final Map<File, SoftReference<CacheLine>> entryCache;

    SharedHttpCacheStorage(File cacheLocation, HttpCacheEntrySerializer serializer, CacheConfig cacheConfig) {

        this.cacheLocation = cacheLocation;
        this.cacheEntrySerializer = serializer;
        entryCache = new LinkedHashMap<File, SoftReference<CacheLine>>(100, 0.75f, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<File, SoftReference<CacheLine>> eldest) {
                if (size() > cacheConfig.getMaxCacheEntries()) {
                    eldest.getValue().clear();
                    return true;
                }
                return false;
            }

        };
    }

    @Override
    public void putEntry(String key, HttpCacheEntry entry) throws IOException {
        getCacheLine(key).putEntry(entry, cacheEntrySerializer);
    }

    private synchronized CacheLine getCacheLine(String key) throws IOException {
        File location = new File(cacheLocation, key.replace("://", "/").replace(':', '/')).getCanonicalFile();
        //we must perform these step here to make sure we always get hold of a strong reference to the cache line, so no Map.compute or similar could be used 
        SoftReference<CacheLine> ref = entryCache.get(location);
        if (ref == null) {
            return createCacheLine(location);
        }
        CacheLine line = ref.get();
        if (line == null) {
            return createCacheLine(location);
        }
        return line;

    }

    private synchronized CacheLine createCacheLine(File location) {
        CacheLine line = new CacheLine(location);
        SoftReference<CacheLine> reference = new SoftReference<>(line);
        entryCache.put(location, reference);
        return line;
    }

    @Override
    public HttpCacheEntry getEntry(String key) throws IOException {
        return getCacheLine(key).getEntry(cacheEntrySerializer);
    }

    @Override
    public void removeEntry(String key) throws IOException {
        getCacheLine(key).delete();
    }

    @Override
    public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException {
        getCacheLine(key).updateEntry(callback, cacheEntrySerializer);
    }

    public void putNotFound(String key) {
        try {
            getCacheLine(key).putNotFound();
        } catch (IOException e) {
            //don't mind then...
        }
    }

    public boolean isNotFound(String key) {
        try {
            long ts = getCacheLine(key).getNotFound();
            return ts > 0;
        } catch (NumberFormatException e) {
            System.err.println(e);
        } catch (IOException e) {
        }
        return false;
    }

    private static final class CacheLine {

        private final File file;
        private final File checkFile;
        private HttpCacheEntry entry;
        private long notFound;

        public CacheLine(File file) {
            this.file = file;
            this.checkFile = new File(file.getParent(), file.getName() + ".lastChecked");
        }

        public synchronized long getNotFound() throws NumberFormatException, IOException {
            if (notFound == 0 && checkFile.isFile()) {
                notFound = Long.parseLong(FileUtils.readFileToString(checkFile, StandardCharsets.US_ASCII));
            }
            return notFound;
        }

        public synchronized void putNotFound() throws IOException {
            notFound = System.currentTimeMillis();
            File parentFile = checkFile.getParentFile();
            if (parentFile.mkdirs() || parentFile.isDirectory()) {
                FileUtils.writeStringToFile(checkFile, String.valueOf(notFound), StandardCharsets.US_ASCII, false);
            }
        }

        public synchronized void updateEntry(HttpCacheUpdateCallback callback, HttpCacheEntrySerializer serializer)
                throws IOException {
            putEntry(callback.update(getEntry(serializer)), serializer);
        }

        public synchronized void putEntry(HttpCacheEntry entry, HttpCacheEntrySerializer serializer)
                throws IOException {
            File parentFile = file.getParentFile();
            if (parentFile.mkdirs() || parentFile.isDirectory()) {
                //TODO file locking for concurrent processes?
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    serializer.writeTo(entry, stream);
                }
            }
            this.entry = entry;
        }

        public synchronized void delete() {
            entry = null;
            file.delete();
        }

        public synchronized HttpCacheEntry getEntry(HttpCacheEntrySerializer serializer) throws IOException {
            if (entry != null) {
                return entry;
            }
            if (file.isFile()) {
                //TODO file locking for concurrent processes?
                try (FileInputStream stream = new FileInputStream(file)) {
                    return entry = serializer.readFrom(stream);
                }
            }
            return null;
        }

    }

}
