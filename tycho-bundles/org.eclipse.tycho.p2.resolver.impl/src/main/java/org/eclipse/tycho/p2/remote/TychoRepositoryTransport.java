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
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.client.cache.DefaultHttpCacheEntrySerializer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.tycho.core.shared.MavenContext;

@SuppressWarnings("restriction")
public class TychoRepositoryTransport extends org.eclipse.equinox.internal.p2.repository.Transport
        implements IAgentServiceFactory {

    private final CloseableHttpClient httpclient;
    private MavenContext mavenContext;
    private SharedHttpCacheStorage cacheStorage;

    public TychoRepositoryTransport(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
        File cacheLocation = new File(mavenContext.getLocalRepositoryRoot(), ".cache/tycho/httpcache");
        cacheLocation.mkdirs();
        mavenContext.getLogger().info(
                "Using TychoRepositoryTransport for remote P2 access with cache location " + cacheLocation + "...");
        CachingHttpClientBuilder builder = CachingHttpClients.custom();
        CacheConfig cacheConfig = CacheConfig.custom()//
                .setMaxCacheEntries(1000)//
                .setMaxObjectSize(1024 * 1024 * 200)//
                .setSharedCache(false).build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).build();
        builder.setDefaultCredentialsProvider(new CredentialsProvider() {

            @Override
            public void setCredentials(AuthScope authscope, Credentials credentials) {

            }

            @Override
            public Credentials getCredentials(AuthScope authscope) {
                // TODO 
                return null;
            }

            @Override
            public void clear() {
            }
        });
        builder.setDefaultRequestConfig(requestConfig);
        cacheStorage = new SharedHttpCacheStorage(cacheLocation, new DefaultHttpCacheEntrySerializer(), cacheConfig);
        builder.setHttpCacheStorage(cacheStorage);
        builder.setCacheConfig(cacheConfig);
        httpclient = builder.build();
    }

    @Override
    public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {
        mavenContext.getLogger().error("??? partial download for " + toDownload);
        return new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(), "not implemented");
    }

    @Override
    public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
        try {
            IOUtils.copy(stream(toDownload, monitor), target);
            return Status.OK_STATUS;
        } catch (AuthenticationFailedException e) {
            return new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
                    "authentication failed for " + toDownload, e);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
                    "download from " + toDownload + " failed", e);
        } catch (CoreException e) {
            return e.getStatus();
        }
    }

    @Override
    public InputStream stream(URI toDownload, IProgressMonitor monitor)
            throws FileNotFoundException, CoreException, AuthenticationFailedException {
        mavenContext.getLogger().info("Requested download for " + toDownload + "...");
        String uri = toDownload.toASCIIString();
        if (cacheStorage.isNotFound(uri)) {
            throw new FileNotFoundException(uri);
        }
        HttpCacheContext context = HttpCacheContext.create();
        HttpGet method = new HttpGet(uri);
        try {
            CloseableHttpResponse result = httpclient.execute(method, context);
            int code = result.getStatusLine().getStatusCode();
            if (code == 404) {
                cacheStorage.putNotFound(uri);
                mavenContext.getLogger().warn("The file was not found!");
                result.close();
                throw new FileNotFoundException(uri);
            }
            if (code == 401) {
                mavenContext.getLogger().error("Authentication Failed!");
                result.close();
                throw new AuthenticationFailedException();
            }
            HttpEntity entity = result.getEntity();
            if (entity == null || code != 200) {
                result.close();
                throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
                        "server returned error (" + result.getStatusLine() + ") for " + uri));
            }
            CacheResponseStatus responseStatus = context.getCacheResponseStatus();
            switch (responseStatus) {
            case CACHE_HIT:
                mavenContext.getLogger()
                        .info("A response was generated from the cache with " + "no requests sent upstream");
                break;
            case CACHE_MODULE_RESPONSE:
                mavenContext.getLogger().info("The response was generated directly by the " + "caching module");
                break;
            case CACHE_MISS:
                mavenContext.getLogger().info("The response came from an upstream server");
                break;
            case VALIDATED:
                mavenContext.getLogger().info("The response was generated from the cache "
                        + "after validating the entry with the origin server");
                break;
            }
            //TODO probably use the AsyncClient instead...
            return new FilterInputStream(entity.getContent()) {
                public void close() throws IOException {
                    super.close();
                    result.close();
                };
            };
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, TychoRepositoryTransport.class.getName(),
                    "download from " + uri + " failed", e));
        }
    }

    @Override
    public long getLastModified(URI toDownload, IProgressMonitor monitor)
            throws CoreException, FileNotFoundException, AuthenticationFailedException {

        return System.currentTimeMillis();
    }

    @Override
    public Object createService(IProvisioningAgent agent) {
        return this;
    }

}
