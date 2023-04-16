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
import java.io.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.toolchain.java.JavaToolchain;
import org.eclipse.tycho.TargetEnvironment;

final class JavaHomeToolchain implements JavaToolchain {

    private String javaHome;

    public JavaHomeToolchain(String javaHome) {
        this.javaHome = javaHome;
    }

    @Override
    public String getType() {
        return ToolchainProvider.TYPE_JDK;
    }

    @Override
    public String findTool(String toolName) {
        File bin = new File(javaHome, "bin");
        if (bin.exists()) {
            File tool;
            if (TargetEnvironment.getRunningEnvironment().isWindows()) {
                tool = new File(bin, toolName + ".exe");
            } else {
                tool = new File(bin, toolName);
            }
            if (tool.exists()) {
                return tool.getAbsolutePath();
            }
            //last resort just in case other extension or case-sensitive file-system...
            File[] listFiles = bin.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile()
                            && FilenameUtils.getBaseName(pathname.getName().toLowerCase()).equals(toolName);
                }
            });
            if (listFiles != null && listFiles.length > 0) {
                return listFiles[0].getAbsolutePath();
            }
        }
        return null;
    }

    public String getJavaHome() {
        return javaHome;
    }

    @Override
    public String toString() {
        return "JDK[" + getJavaHome() + "]";
    }

}
