/*******************************************************************************
 * Copyright (c) 2013, 2014 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

public class TocOptions {
    private String mainLabel = "API Reference";
    private String mainFilename = "overview-summary.html";

    public void setMainLabel(final String label) {
        this.mainLabel = label;
    }

    public String getMainLabel() {
        return this.mainLabel;
    }

    public void setMainFilename(final String location) {
        this.mainFilename = location;

    }

    public String getMainFilename() {
        return this.mainFilename;
    }
}
