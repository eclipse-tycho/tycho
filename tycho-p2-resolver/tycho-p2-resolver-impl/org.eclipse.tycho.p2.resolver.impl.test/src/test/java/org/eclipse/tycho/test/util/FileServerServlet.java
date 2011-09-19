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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FileServerServlet extends AbstractMonitorServlet {
    private static final long serialVersionUID = -6702619558275132007L;

    private File content;

    public FileServerServlet(File content) {
        this.content = content;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getPathInfo();

        File file = new File(content, path);
        if (!file.exists()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found " + file.getAbsolutePath());
            return;
        }
        if (!file.isFile()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Directory not accessible " + file.getAbsolutePath());
            return;
        }

        addUri(req);

        InputStream input = new FileInputStream(file);
        try {
            OutputStream output = res.getOutputStream();
            try {
                final byte[] buffer = new byte[10240];
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
    }

}
