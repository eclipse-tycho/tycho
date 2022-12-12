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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractMonitorServlet extends GenericServlet {

    private static final long serialVersionUID = -7271350776954812609L;

    private final List<String> accessedUrls;

    public AbstractMonitorServlet() {
        this.accessedUrls = new ArrayList<>();
    }

    public List<String> getAccessedUrls() {
        return this.accessedUrls;
    }

    @Override
    public final void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI().toString();
        if (!uri.endsWith(".sha1") && !uri.endsWith(".md5")) {
            accessedUrls.add(uri);
        }

        service(req, res);
    }

    protected void addUri(HttpServletRequest req) {
        String uri = req.getRequestURI().toString();
        if (!accessedUrls.contains(uri)) {
            accessedUrls.add(uri);
        }
    }

    public abstract void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException;
}
