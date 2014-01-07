/*******************************************************************************
 * Copyright (c) 2013 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.util.LinkedList;
import java.util.List;

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

    private List<String> jvmOptions = new LinkedList<String>();

    private List<String> additionalArguments = new LinkedList<String>();

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
}
