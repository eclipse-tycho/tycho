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
import java.net.BindException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

public class HttpServer {
    private static final int BIND_ATTEMPTS = 20;

    private static final Random rnd = new Random();

    private final Server server;

    private final ServletContextHandler context;

    private final int port;

    private final Map<String, FileServerServlet> servers = new HashMap<String, FileServerServlet>();

    private HttpServer(int port, Server server, ServletContextHandler context) {
        this.port = port;
        this.server = server;
        this.context = context;
    }

    public static HttpServer startServer() throws Exception {
        return startServer(null, null);
    }

    public static HttpServer startServer(String username, String password) throws Exception {
        int baseport = 1024;
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
        Connector connector = new SocketConnector();
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context;
        if (username != null) {
            context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS
                    | ServletContextHandler.SECURITY);

            HashLoginService userRealm = new HashLoginService("default");
            userRealm.putUser(username, new Password(password), new String[0]);

            Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, Constraint.ANY_ROLE);
            constraint.setAuthenticate(true);

            ConstraintMapping constraintMapping = new ConstraintMapping();
            constraintMapping.setPathSpec("/*");
            constraintMapping.setConstraint(constraint);

            ConstraintSecurityHandler securityHandler = (ConstraintSecurityHandler) context.getSecurityHandler();
            securityHandler.setLoginService(userRealm);
            securityHandler.setAuthMethod(Constraint.__BASIC_AUTH);
            securityHandler.setConstraintMappings(new ConstraintMapping[] { constraintMapping });
        } else {
            context = new ServletContextHandler(server, "/", 0);
        }
        server.start();

        return new HttpServer(port, server, context);
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    public String addServer(String contextName, final File content) {
        FileServerServlet servlet = new FileServerServlet(content);
        servers.put(contextName, servlet);
        context.addServlet(new ServletHolder(servlet), "/" + contextName + "/*");

        return getUrl(contextName);
    }

    public String getUrl(String contextName) {
        return "http://localhost:" + port + "/" + contextName;
    }

    public List<String> getAccessedUrls(String contextName) {
        return servers.get(contextName).getAccessedUrls();
    }

    public void clearAccessedUrls(String contextName) {
        servers.get(contextName).getAccessedUrls().clear();
    }

}
