/*******************************************************************************
 * Copyright (c) 2013, 2015 IBH SYSTEMS GmbH and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;

/**
 * The javadoc options<br/>
 * At the moment the list of real options is quite small, but most arguments can be passed using the
 * <code>additionalArguments</code> property.
 * 
 * @author Jens Reimann
 */
public class JavadocOptions {
    private String executable;

    private boolean ignoreError = true;

    private List<String> jvmOptions = new LinkedList<>();

    private List<String> additionalArguments = new LinkedList<>();

    private List<Dependency> docletArtifacts = new LinkedList<>();

    private String doclet;

    private String encoding;

    private List<String> includes = new LinkedList<>();

    private List<String> excludes = new LinkedList<>();

    public void setIgnoreError(final boolean ignoreError) {
        this.ignoreError = ignoreError;
    }

    public boolean isIgnoreError() {
        return this.ignoreError;
    }

    public void setExecutable(final String executable) {
        this.executable = executable;
    }

    public String getExecutable() {
        return this.executable;
    }

    public List<String> getJvmOptions() {
        return this.jvmOptions;
    }

    public void setJvmOptions(final List<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public List<String> getAdditionalArguments() {
        return this.additionalArguments;
    }

    public void setAdditionalArguments(final List<String> additionalArguments) {
        this.additionalArguments = additionalArguments;
    }

    public List<Dependency> getDocletArtifacts() {
        return this.docletArtifacts;
    }

    public void setDocletArtifacts(List<Dependency> docletArtifacts) {
        this.docletArtifacts = docletArtifacts;
    }

    public String getDoclet() {
        return doclet;
    }

    public void setDoclet(String doclet) {
        this.doclet = doclet;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

}
