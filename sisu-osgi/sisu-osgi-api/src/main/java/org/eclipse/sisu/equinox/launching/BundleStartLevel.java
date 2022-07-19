/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.sisu.equinox.launching;

public class BundleStartLevel {
    /**
     * Bundle symbolic name.
     */
    private String id;

    /**
     * Bundle start level. level==-1 is used to remove org.eclipse.org bundle from osgi.bundles
     * system property value.
     */
    private int level;

    /**
     * Bundle auto start.
     */
    private boolean autoStart;

    public BundleStartLevel() {
        // default constructor used by mojo parameter injection
    }

    public BundleStartLevel(String id, int level, boolean autoStart) {
        this.id = id;
        this.level = level;
        this.setAutoStart(autoStart);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoStart() {
        return autoStart;
    }
}
