/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.testutil;

import java.io.File;
import java.net.BindException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.rules.ExternalResource;

public class HttpServer extends ExternalResource {
    static final int BIND_ATTEMPTS = 20;
    static final Random rnd = new Random();

    private RunningServer runningServer;

    private static class RunningServer {

        final Server server;
        final ServletContextHandler context;
        final int port;
        final Map<String, FileServerServlet> servlets = new HashMap<>();

        RunningServer(int port, Server jettyIntance, ServletContextHandler context) {
            this.port = port;
            this.server = jettyIntance;
            this.context = context;
        }

    }

    @Override
    protected void before() throws Throwable {
        runningServer = startServer();
    }

    @Override
    protected void after() {
        try {
            stop();
        } catch (Exception e) {
            // only log to not hide test result
            e.printStackTrace();
        }
    }

    private static RunningServer startServer() throws Exception {
        int baseport = 1024;
        BindException cause = null;
        for (int i = 0; i < BIND_ATTEMPTS; i++) {
            int port = baseport + rnd.nextInt(65534 - baseport);
            try {
                return startServerOnPort(port);
            } catch (BindException e) {
                cause = e;
            }
        }

        throw new IllegalStateException("Could not allocate available port", cause);
    }

    private static RunningServer startServerOnPort(int port) throws Exception {
        Server jetty = new Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("localhost");
        connector.setPort(port);
        jetty.addConnector(connector);

        ServletContextHandler context;
        context = new ServletContextHandler(jetty, "/", 0);
        jetty.start();

        return new RunningServer(port, jetty, context);
    }

    public String addServlet(String contextName, final File content) {
        checkRunning();

        FileServerServlet servlet = new FileServerServlet(content);
        runningServer.servlets.put(contextName, servlet);
        runningServer.context.addServlet(new ServletHolder(servlet), "/" + contextName + "/*");

        return getUrl(contextName);
    }

    public String getUrl(String contextName) {
        checkRunning();

        return "http://localhost:" + runningServer.port + "/" + contextName;
    }

    public List<String> getAccessedUrls(String contextName) {
        checkRunning();

        return runningServer.servlets.get(contextName).getAccessedUrls();
    }

    public void clearAccessedUrls(String contextName) {
        checkRunning();

        runningServer.servlets.get(contextName).getAccessedUrls().clear();
    }

    public void stop() throws Exception {
        if (runningServer != null) {
            runningServer.server.stop();
            runningServer.server.join();
        }
        runningServer = null;
    }

    private void checkRunning() {
        if (runningServer == null) {
            throw new IllegalStateException("HttpServer instance is not running. Did you forget the @Rule annotation?");
        }
    }

}
