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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.toolchain.java.JavaToolchain;
import org.eclipse.tycho.TargetEnvironment;

final class JavaHomeToolchain implements JavaToolchain {

    private Path javaHome;

    public JavaHomeToolchain(String javaHome) {
        this.javaHome = Path.of(javaHome).toAbsolutePath();
    }

    @Override
    public String getType() {
        return ToolchainProvider.TYPE_JDK;
    }

    @Override
    public String findTool(String toolName) {
        Path bin = javaHome.resolve("bin");
        if (Files.isDirectory(bin)) {
            Path tool = bin.resolve(toolName + (TargetEnvironment.getRunningEnvironment().isWindows() ? ".exe" : ""));
            if (Files.isRegularFile(tool)) {
                return tool.toString();
            }
            //last resort just in case other extension or case-sensitive file-system...
            try (var files = Files.list(bin).filter(Files::isRegularFile)) {
                return files.map(Path::toString)
                        .filter(pathname -> FilenameUtils.getBaseName(pathname).equalsIgnoreCase(toolName)).findFirst()
                        .orElse(null);
            } catch (IOException e) {
            }
        }
        return null;
    }

    @Override
    public String getJavaHome() {
        return javaHome.toString();
    }

    @Override
    public ArtifactVersion getJavaVersion() {
        Path release = javaHome.resolve("release");
        if (Files.isRegularFile(release)) {
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(release)) {
                properties.load(reader);
                String javaVersion = properties.getProperty("JAVA_VERSION");
                if (javaVersion != null) {
                    return new DefaultArtifactVersion(javaVersion.replace("\"", ""));
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "JDK[" + getJavaHome() + "]";
    }

}
