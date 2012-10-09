/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging.sourceref;

import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Maven SCM URL as specified by {@linkplain http://maven.apache.org/scm/scm-url-format.html}
 */
public class ScmUrl {

    private static final String SCM_CONNECTION_PROPERTY = "tycho.scmUrl";
    private String type;
    private String url;

    public ScmUrl(Properties projectProperties) throws MojoExecutionException {
        this.url = projectProperties.getProperty(SCM_CONNECTION_PROPERTY);
        if (url == null) {
            throw new MojoExecutionException("Eclipse-SourceReferences header should be generated but ${"
                    + SCM_CONNECTION_PROPERTY + "} is not set");
        }
        if (!url.startsWith("scm:")) {
            throw new MojoExecutionException("Invalid SCM URL: '" + url
                    + "'. See http://maven.apache.org/scm/scm-url-format.html");
        }
        int delimiterIndex = -1;
        int pipeIndex = url.indexOf('|', 4);
        int colonIndex = url.indexOf(':', 4);
        if (pipeIndex > 0) {
            if (colonIndex > 0) {
                delimiterIndex = Math.min(pipeIndex, colonIndex);
            } else {
                delimiterIndex = pipeIndex;
            }
        } else {
            delimiterIndex = colonIndex;
        }
        if (delimiterIndex == -1) {
            throw new MojoExecutionException("Invalid SCM URL: '" + url
                    + "'. See http://maven.apache.org/scm/scm-url-format.html");
        }
        this.type = url.substring(4, delimiterIndex);
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }
}
