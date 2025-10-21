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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.inject.Named;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.osgi.OSGiFramework;
import org.eclipse.tycho.osgi.OSGiFrameworkLauncher;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * The {@link EmbeddedFrameworkLauncher} launches a framework in the same JVM
 */
@Named("embedded")
public class EmbeddedFrameworkLauncher implements OSGiFrameworkLauncher {

    @Override
    public OSGiFramework launchFramework(MavenProject project, Map<String, String> properties) throws IOException {
        if (properties == null) {
            properties = Map.of();
        }
        Path workDir = prepareWorkdir(project, properties);
        Map<String, String> map = createProperties(properties, workDir);
        map.put("osgi.classloader.copy.natives", "true");
        FrameworkFactory factory = getFrameworkFactory(FrameworkFactory.class);
        Framework framework = factory.newFramework(map);
        try {
            framework.init();
        } catch (BundleException e) {
            throw new IOException("Initialize the framework failed!", e);
        }
        try {
            framework.start();
        } catch (BundleException e) {
            throw new IOException("Start the framework failed!", e);
        }
        return new EmbeddedOSGiFramework(framework, Boolean.parseBoolean(properties.get(STANDALONE)));
    }

    static Map<String, String> createProperties(Map<String, String> properties, Path workDir) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("osgi.configuration.area", workDir.resolve("configuration").toAbsolutePath().toString());
        map.put("osgi.instance.area", workDir.resolve("data").toAbsolutePath().toString());
        map.put("osgi.compatibility.bootdelegation", "true");
        map.put("osgi.framework.useSystemProperties", "false");
        map.put("eclipse.ignoreApp", "true");
        map.put("osgi.noShutdown", "true");
        String sl = properties.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
        if (sl != null) {
            map.put("osgi.bundles.defaultStartLevel", sl);
        }
        if (properties != null) {
            map.putAll(properties);
        }
        map.remove(Constants.FRAMEWORK_STORAGE);
        return map;
    }

    static Path prepareWorkdir(MavenProject project, Map<String, String> properties) throws IOException {
        String storage = properties.get(Constants.FRAMEWORK_STORAGE);
        Path workDir;
        if (storage != null) {
            workDir = Path.of(storage);
        } else {
            if (project == null) {
                workDir = Files.createTempDirectory("embedded");
            } else {
                workDir = Files.createTempDirectory(Path.of(project.getBuild().getDirectory()), "embedded");
            }
        }
        cleanWorkDir(workDir);
        Files.createDirectories(workDir);
        return workDir;
    }

    static <T> T getFrameworkFactory(Class<T> type) throws IOException {
        ServiceLoader<T> loader = ServiceLoader.load(type, EmbeddedFrameworkLauncher.class.getClassLoader());
        return loader.findFirst().orElseThrow(() -> new IOException("No FrameworkFactory found on classpath"));
    }

    private static void cleanWorkDir(Path workDir) {
        try {
            Files.walkFileTree(workDir, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
        }
    }

}
