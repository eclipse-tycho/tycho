/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

public class HttpServer {
    private static class MonitoringServlet extends DefaultServlet {
        private List<String> accessedURIs = new ArrayList<String>();

        @Override
        public String getInitParameter(String name) {
            // no directory listing allowed
            if ("dirAllowed".equals(name)) {
                return "false";
            } else {
                return super.getInitParameter(name);
            }
        }

        public List<String> getAccessedURIs() {
            return accessedURIs;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            accessedURIs.add(((Request) request).getUri().toString());
            super.doGet(request, response);
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
                IOException {
            accessedURIs.add(((Request) request).getUri().toString());
            super.doPost(request, response);
        }
    }

    private static final int BIND_ATTEMPTS = 20;

    private static final Random rnd = new Random();

    private final Server server;

    private final int port;

    private final Map<String, MonitoringServlet> contextName2servletsMap = new HashMap<String, MonitoringServlet>();

    private ContextHandlerCollection contexts;

    private HttpServer(int port, Server server, ContextHandlerCollection contexts) {
        this.port = port;
        this.server = server;
        this.contexts = contexts;
    }

    public static HttpServer startServer() throws Exception {
        return startServer(null, null);
    }

    public static HttpServer startServer(String username, String password) throws Exception {
        int baseport = EnvironmentUtil.getHttpServerPort();
        BindException cause = null;
        for (int i = 0; i < BIND_ATTEMPTS; i++) {
            int port = baseport + rnd.nextInt(65534 - baseport);
            try {
                return doStartServer(username, password, port);
            } catch (BindException e) {
                cause = e;
            }
        }

        throw new IllegalStateException("Could not allocate available port", cause);
    }

    private static HttpServer doStartServer(String username, String password, int port) throws Exception {
        Server server = new Server();

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        Connector connector = new SocketConnector();
        connector.setPort(port);
        server.addConnector(connector);

        if (username != null) {
            HashLoginService userRealm = new HashLoginService("default");
            userRealm.putUser(username, new Password(password), new String[] { Constraint.ANY_ROLE });

            Constraint constraint = new Constraint("default", Constraint.ANY_ROLE);
            constraint.setAuthenticate(true);
            ConstraintMapping constraintMapping = new ConstraintMapping();
            constraintMapping.setPathSpec("/*");
            constraintMapping.setConstraint(constraint);

            ConstraintSecurityHandler securedHandler = new ConstraintSecurityHandler();
            securedHandler.setAuthenticator(new BasicAuthenticator());
            securedHandler.addConstraintMapping(constraintMapping);
            securedHandler.setLoginService(userRealm);

            // chain handlers together
            securedHandler.setHandler(contexts);
            server.setHandler(securedHandler);
        }

        server.start();
        return new HttpServer(port, server, contexts);
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    public String addServer(String contextName, final File content) {
        ServletContextHandler context = new ServletContextHandler(contexts, URIUtil.SLASH + contextName);
        context.setResourceBase(content.getAbsolutePath());

        MonitoringServlet monitoringServlet = new MonitoringServlet();
        contextName2servletsMap.put(contextName, monitoringServlet);
        context.addServlet(new ServletHolder(monitoringServlet), URIUtil.SLASH);
        contexts.addHandler(context);
        try {
            context.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return getUrl(contextName);
    }

    public String getUrl(String contextName) {
        return "http://localhost:" + port + "/" + contextName;
    }

    public List<String> getAccessedUrls(String contextName) {
        return contextName2servletsMap.get(contextName).getAccessedURIs();
    }

}
