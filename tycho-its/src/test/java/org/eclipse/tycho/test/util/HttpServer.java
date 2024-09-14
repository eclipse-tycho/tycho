/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.NetworkTrafficServerConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;

public class HttpServer {

	private class Monitoring implements Filter {
		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			Request httprequest = Request.getBaseRequest(request);
			String context = httprequest.getContextPath();
			assert context.startsWith("/");
			context = context.substring(1);
			synchronized (contextName2accessedUrls) {
				contextName2accessedUrls.add(context, httprequest.getServletPath());
			}
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {

		}
	}

	private static class RedirectServlet extends HttpServlet {
		private final Function<String, String> relativeUrlToNewUrl;

		public RedirectServlet(Function<String, String> relativeUrlToNewUrl2) {
			super();
			this.relativeUrlToNewUrl = relativeUrlToNewUrl2;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.sendRedirect(relativeUrlToNewUrl.apply(req.getServletPath()));
		}

	}

	private static final int BIND_ATTEMPTS = 20;

	private final Server server;

	private final int port;

	private final MultiMap<String> contextName2accessedUrls = new MultiMap<>();

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
		IllegalStateException exception = new IllegalStateException("Could not allocate a port");
		for (int i = 0; i < BIND_ATTEMPTS; i++) {
			int port = baseport + i;
			try {
				return doStartServer(username, password, port);
			} catch (IOException e) {
				exception.addSuppressed(e);
				TimeUnit.SECONDS.sleep(1);
			}
		}
		throw exception;
	}

	private static HttpServer doStartServer(String username, String password, int port) throws Exception {
		Server server = new Server();

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler(contexts);

		ServerConnector connector = new NetworkTrafficServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		if (username != null) {
			HashLoginService userRealm = new HashLoginService("default");
			UserStore userStore = new UserStore();
			userRealm.setUserStore(userStore);
			userStore.addUser(username, new Password(password), new String[] { "user" });

			Constraint constraint = new Constraint("default", Constraint.ANY_ROLE);
			constraint.setAuthenticate(true);
			constraint.setRoles(new String[] { "user" });
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
		connector.open();
		return new HttpServer(port, server, contexts);
	}

	public void stop() throws Exception {
		server.stop();
		server.join();
	}

	public String addServer(String contextName, final File content) {
		return addServer(contextName, DefaultServlet.class, content);
	}

	public String addServer(String contextName, Class<? extends Servlet> servlet, final File content) {
		ServletContextHandler context = new ServletContextHandler(contexts, URIUtil.SLASH + contextName);
		context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		context.setResourceBase(content.getAbsolutePath());
		context.addServlet(servlet, URIUtil.SLASH);
		registerContext(context);
		return getUrl(contextName);
	}

	/**
	 * 
	 * @param contextName         - a path prefix to handle
	 * @param relativeUrlToNewUrl - redirecting function. Takes a path within
	 *                            context starting with slash and return an new
	 *                            absolute URL to redirect to (Location header
	 *                            value).
	 * @return an URL prefix to redirect from
	 */
	public String addRedirect(String contextName, Function<String, String> relativeUrlToNewUrl) {
		ServletContextHandler context = new ServletContextHandler(contexts, URIUtil.SLASH + contextName);
		context.addServlet(new ServletHolder(new RedirectServlet(relativeUrlToNewUrl)), URIUtil.SLASH);
		registerContext(context);
		return getUrl(contextName);

	}

	public String getUrl(String contextName) {
		return "http://localhost:" + port + "/" + contextName;
	}

	public List<String> getAccessedUrls(String contextName) {
		synchronized (contextName2accessedUrls) {
			return List.copyOf(contextName2accessedUrls.getOrDefault(contextName, Collections.emptyList()));
		}
	}

	/**
	 * Reset state.
	 * 
	 * Clear access logs. Does not affect configuration.
	 **/
	public void clear() {
		synchronized (contextName2accessedUrls) {
			contextName2accessedUrls.clear();
		}
	}

	private void registerContext(ServletContextHandler context) {
		context.addFilter(new FilterHolder(new Monitoring()), "*", EnumSet.of(DispatcherType.REQUEST));
		contexts.addHandler(context);
		try {
			context.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
