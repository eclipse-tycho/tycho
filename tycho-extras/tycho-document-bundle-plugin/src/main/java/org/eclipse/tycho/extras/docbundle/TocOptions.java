/*******************************************************************************
 * Copyright (c) 2013, 2014 IBH SYSTEMS GmbH and others.
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
