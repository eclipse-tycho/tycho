/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.io.File;

import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.JavaToolchainImpl;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class OSGiJavaToolchain implements Toolchain {

    private Toolchain base;

    public OSGiJavaToolchain(Toolchain base) {
        this.base = base;
    }

    @Override
    public String getType() {
        return base.getType();
    }

    @Override
    public String findTool(String toolName) {
        return base.findTool(toolName);
    }

    public String getJavaHome() {
        if (base instanceof JavaToolchainImpl defaultToolchain) {
            return defaultToolchain.getJavaHome();
        }
        if (base instanceof JavaHomeToolchain javaHomeToolchain) {
            return javaHomeToolchain.getJavaHome();
        }
        String tool = findTool("java");
        if (tool != null) {
            File javaHome = new File(tool).getParentFile().getParentFile();
            return javaHome.getAbsolutePath();
        }
        return null;
    }

    public Xpp3Dom getConfiguration() {
        if (base instanceof ToolchainPrivate privateToolchain
                && privateToolchain.getModel().getConfiguration() instanceof Xpp3Dom xpp3) {
            return xpp3;
        }
        return null;
    }

    @Override
    public String toString() {
        return base.toString();
    }

}
