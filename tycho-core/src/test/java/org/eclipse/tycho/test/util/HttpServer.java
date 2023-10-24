/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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
        int baseport = getHttpServerPort();
        IllegalStateException exception = new IllegalStateException("Could not allocate a port");
        for (int i = 0; i < BIND_ATTEMPTS; i++) {
            int port = baseport + i;
            try {
                return startServerOnPort(port);
            } catch (BindException e) {
                exception.addSuppressed(e);
                TimeUnit.SECONDS.sleep(1);
            }
        }
        throw exception;
    }

    public static int getHttpServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();
            if (localPort > 0) {
                return localPort;
            }
        } catch (IOException e) {
        }
        return 1024;
    }

    private static RunningServer startServerOnPort(int port) throws Exception {
        Server jetty = new Server();
        ServerConnector connector = new ServerConnector(jetty);
        connector.setHost("localhost");
        connector.setPort(port);
        jetty.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler("/", 0);
        jetty.setHandler(context);
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
