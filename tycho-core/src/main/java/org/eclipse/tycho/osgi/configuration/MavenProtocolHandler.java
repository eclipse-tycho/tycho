/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.tycho.osgi.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.sisu.equinox.embedder.EmbeddedEquinox;
import org.eclipse.sisu.equinox.embedder.EquinoxLifecycleListener;
import org.eclipse.tycho.core.shared.MavenDependenciesResolver;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

@Component(role = EquinoxLifecycleListener.class, hint = "MavenProtocolHandler")
public class MavenProtocolHandler extends EquinoxLifecycleListener {

    @Override
    public void afterFrameworkStarted(EmbeddedEquinox framework) {

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { "mvn" });
        framework.registerService(URLStreamHandlerService.class,
                new MvnProtocolHandlerService(framework.getServiceFactory()), properties);
    }

    private class MvnProtocolHandlerService extends AbstractURLStreamHandlerService {

        private EquinoxServiceFactory serviceFactory;

        public MvnProtocolHandlerService(EquinoxServiceFactory serviceFactory) {
            this.serviceFactory = serviceFactory;
        }

        public URLConnection openConnection(URL url) {
            return new MavenURLConnection(url, serviceFactory.getService(MavenDependenciesResolver.class));
        }
    }

    private static final class MavenURLConnection extends URLConnection {

        private MavenDependenciesResolver resolver;
        private String subPath;
        private IArtifactFacade artifactFacade;

        protected MavenURLConnection(URL url, MavenDependenciesResolver dependenciesResolver) {
            super(url);
            this.resolver = dependenciesResolver;
        }

        @Override
        public void connect() throws IOException {
            if (artifactFacade != null) {
                return;
            }
            if (resolver == null) {
                throw new IOException("resolver service is not available");
            }
            String path = url.getPath();
            if (path == null) {
                throw new IOException("maven coordinates are missing");
            }
            int subPathIndex = path.indexOf('/');
            String[] coordinates;
            if (subPathIndex > -1) {
                subPath = path.substring(subPathIndex);
                coordinates = path.substring(0, subPathIndex).split(":");
            } else {
                coordinates = path.split(":");
            }
            if (coordinates.length < 3) {
                throw new IOException("required format is groupId:artifactId:version[:packaging[:classifier]]");
            }
            String type = coordinates.length > 3 ? coordinates[3] : "jar";
            String classifier = coordinates.length > 4 ? coordinates[4] : null;
            Collection<?> resolve = resolver.resolve(coordinates[0], coordinates[1], coordinates[2], type, classifier,
                    null);
            if (resolve.isEmpty()) {
                throw new IOException("artifact " + Arrays.toString(coordinates)
                        + " could not be downloaded from any of the available repositories");
            }
            if (resolve.size() > 1) {
                throw new IOException("artifact " + Arrays.toString(coordinates) + " resolves to multiple artifacts");
            }
            artifactFacade = (IArtifactFacade) resolve.iterator().next();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            File location = artifactFacade.getLocation();
            if (subPath == null) {
                return new FileInputStream(location);
            }
            String urlSpec = "jar:" + location.toURI() + "!" + subPath;
            return new URL(urlSpec).openStream();
        }

    }

}
