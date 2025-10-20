/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *    
 */
package org.eclipse.tycho.osgi.impl;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.osgi.OSGiFramework;
import org.eclipse.tycho.osgi.OSGiFrameworkLauncher;
import org.eclipse.tycho.osgi.impl.ForkedFrameworkMain.SocketCommandChannel;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;

/**
 * The {@link ForkedFrameworkLauncher} launches a framework in a new JVM process
 */
@Named("forked")
public class ForkedFrameworkLauncher implements OSGiFrameworkLauncher {

    @Override
    public OSGiFramework launchFramework(MavenProject project, Map<String, String> properties) throws IOException {
        Path workDir = EmbeddedFrameworkLauncher.prepareWorkdir(project, properties);
        Map<String, String> map = EmbeddedFrameworkLauncher.createProperties(properties, workDir);
        Path path = getJavaExecutable();
        ServerSocket serverSocket = new ServerSocket(0);
        List<String> commandline = new ArrayList<>();
        commandline.add(path.toAbsolutePath().toString()); // java executable
        commandline.add("-cp");
        commandline.add(getClasspathFor(ForkedFrameworkMain.class, //our jar itself...
                EmbeddedFrameworkLauncher.getFrameworkFactory(ConnectFrameworkFactory.class).getClass(), //osgi impl
                Framework.class // API
        ));
        commandline.add(ForkedFrameworkMain.class.getName());
        commandline.add(Integer.toString(serverSocket.getLocalPort()));
        ProcessBuilder pb = new ProcessBuilder(commandline);
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        Process process = pb.start();
        process.onExit().thenRun(() -> {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        });
        return new ForkedOSGiFramework(process, map, new SocketCommandChannel(serverSocket.accept()));
    }

    private String getClasspathFor(Class<?>... classes) throws IOException {
        Set<String> cp = new LinkedHashSet<>();
        for (Class<?> clz : classes) {
            cp.add(getJarFor(clz));
        }
        return cp.stream().collect(Collectors.joining(File.pathSeparator));
    }

    static String getJarFor(Class<?> marker) throws IOException {
        try {
            return new File(marker.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private Path getJavaExecutable() throws IOException {
        String property = System.getProperties().getProperty("java.home");
        if (property == null) {
            throw new IOException("java home not defined");
        }
        Path javaHome = Path.of(property);
        Path java = javaHome.resolve("bin/java");
        if (Files.isRegularFile(java)) {
            return java;
        }
        Path javaExe = javaHome.resolve("bin/java.exe");
        if (Files.isRegularFile(javaExe)) {
            return javaExe;
        }
        throw new IOException("No java executable found in " + javaHome);
    }

}
