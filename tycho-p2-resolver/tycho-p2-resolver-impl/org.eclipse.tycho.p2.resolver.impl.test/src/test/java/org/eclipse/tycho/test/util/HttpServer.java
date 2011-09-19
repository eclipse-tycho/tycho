/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.Password;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class HttpServer {
    private static final int BIND_ATTEMPTS = 20;

    private static final Random rnd = new Random();

    private final Server server;

    private final Context context;

    private final int port;

    private final Map<String, FileServerServlet> servers = new HashMap<String, FileServerServlet>();

    private HttpServer(int port, Server server, Context context) {
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
        server.start();

        Context context;
        if (username != null) {
            context = new Context(server, "/", Context.SESSIONS | Context.SECURITY);

            HashUserRealm userRealm = new HashUserRealm("default");
            userRealm.put(username, new Password(password));

            Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, Constraint.ANY_ROLE);
            constraint.setAuthenticate(true);

            ConstraintMapping constraintMapping = new ConstraintMapping();
            constraintMapping.setPathSpec("/*");
            constraintMapping.setConstraint(constraint);

            context.getSecurityHandler().setUserRealm(userRealm);
            context.getSecurityHandler().setAuthMethod(Constraint.__BASIC_AUTH);
            context.getSecurityHandler().setConstraintMappings(new ConstraintMapping[] { constraintMapping });
        } else {
            context = new Context(server, "/", 0);
        }
        context.start();

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

}
