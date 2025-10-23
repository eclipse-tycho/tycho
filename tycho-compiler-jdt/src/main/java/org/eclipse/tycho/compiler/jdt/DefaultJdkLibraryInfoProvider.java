/*******************************************************************************
 * Copyright (c) 2018, 2024 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.compiler.jdt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.LegacySupport;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;
import org.eclipse.tycho.core.maven.MavenDependenciesResolver;
import org.eclipse.tycho.version.TychoVersion;

/**
 * Determine and cache system library info (Java version, bootclasspath, extension and endorsed
 * directories) for given javaHome directories.
 */
@Named
@Singleton
public class DefaultJdkLibraryInfoProvider implements JdkLibraryInfoProvider {

    private static final String TYCHO_LIB_DETECTOR_VERSION = System.getProperty("tycho.libdetector.version",
            TychoVersion.getTychoVersion());

    @Inject
    private LegacySupport legacySupport;

    @Inject
    private MavenDependenciesResolver dependenciesResolver;

    @Inject
    private Logger log;

    private Map<String, LibraryInfo> libraryInfoCache = new HashMap<>();
    private File libDetectorJar;
    private Boolean isRunningOnJava9orLater;

    @Override
    public LibraryInfo getLibraryInfo(String javaHome) throws ArtifactResolutionException {
        LibraryInfo libInfo = libraryInfoCache.get(javaHome);
        if (libInfo == null) {
            libInfo = generateLibraryInfo(javaHome);
            libraryInfoCache.put(javaHome, libInfo);
        }
        return libInfo;
    }

    private LibraryInfo generateLibraryInfo(String javaHome) throws ArtifactResolutionException {
        String executable = javaHome + File.separator + "bin" + File.separator + "java";
        if (File.separatorChar == '\\') {
            executable = executable + ".exe";
        }
        if (!new File(executable).isFile()) {
            getLog().warn(executable + " not found. Fallback to scan " + javaHome + "/lib/*.jar and " + javaHome
                    + "/lib/ext/*.jar for bootclasspath");
            return new LibraryInfo("unknown", scanLibFolders(javaHome), new String[0], new String[0]);
        }
        CommandLine cli = new CommandLine(executable);
        cli.addArguments(new String[] { "-classpath", getLibDetectorJar().getAbsolutePath(),
                "org.eclipse.tycho.libdetector.LibraryDetector" }, false);
        DefaultExecutor executor = DefaultExecutor.builder().get();
        ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofMillis(30 * 1000L)).get();
        executor.setWatchdog(watchdog);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler handler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(handler);
        int exitValue = -1;
        try {
            exitValue = executor.execute(cli);
        } catch (ExecuteException e) {
            if (watchdog.killedProcess()) {
                throw new RuntimeException("Timeout 30 s exceeded. Commandline " + cli.toString()
                        + " was killed. Output: " + outputStream.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (exitValue == 0) {
            return parseLibraryInfo(outputStream.toString(), javaHome);
        } else {
            throw new RuntimeException(
                    cli.toString() + " process exit code was " + exitValue + ". Output: " + outputStream.toString());
        }

    }

    private static String[] scanLibFolders(String javaHome) {
        List<String> bootclasspathList = new ArrayList<>();
        addJarsToList(scanForJars(new File(javaHome, "lib")), bootclasspathList);
        addJarsToList(scanForJars(new File(javaHome, "lib/ext")), bootclasspathList);
        return bootclasspathList.toArray(new String[0]);
    }

    private static void addJarsToList(File[] jars, List<String> fileList) {
        for (File jar : jars) {
            fileList.add(jar.getAbsolutePath());
        }
    }

    private static File[] scanForJars(File libDir) {
        if (!libDir.isDirectory()) {
            return new File[0];
        }
        return libDir.listFiles((FilenameFilter) (dir, name) -> name.endsWith(".jar") && new File(dir, name).isFile());
    }

    private LibraryInfo parseLibraryInfo(String output, String javaHome) {
        // JVM may add garbage before and/or after our output, strip it
        String[] splitPrefixAndSuffix = output.split(Pattern.quote("####"));
        if (splitPrefixAndSuffix.length < 2) {
            throw new IllegalStateException("Could not parse process output: " + output);
        }
        output = splitPrefixAndSuffix[1];
        String[] parts = output.split(Pattern.quote("|"));
        if (parts.length != 4) {
            throw new IllegalStateException("Could not parse process output: " + output);
        }
        String javaVersion = parts[0];
        String[] bootclasspath = splitPath(parts[1]);
        String[] extDirs = splitPath(parts[2]);
        String[] endorsedDirs = splitPath(parts[3]);
        // workaround for missing bootclasspath in Java 9 - see JDT bug https://eclip.se/489207  
        if (bootclasspath.length == 0) {
            File jrtFsJar = new File(javaHome, "lib/jrt-fs.jar");
            if (!jrtFsJar.isFile()) {
                jrtFsJar = new File(javaHome, "jrt-fs.jar");
                if (!jrtFsJar.isFile()) {
                    throw new IllegalStateException("jrt-fs.jar not found in " + javaHome);
                }
            }
            bootclasspath = new String[] { jrtFsJar.getAbsolutePath() };
        }
        if (isRunningOnJava9orLater()) {
            // JDT APT throws IllegalArgumentException for "-extdirs" CLI arg on Java 9
            // TODO bug in JDT? -extdirs should only be disallowed if used with --release
            // according to https://docs.oracle.com/javase/9/tools/javac.htm#GUID-AEEC9F07-CB49-4E96-8BC7-BCC2C7F725C9__STANDARDOPTIONSFORJAVAC-7D3D9CC2 
            extDirs = new String[0];
        }
        //sometimes the jvm report entries that do not really exits...
        String[] filteredBcp = Arrays.stream(bootclasspath).filter(bcp -> new File(bcp).exists())
                .toArray(String[]::new);
        return new LibraryInfo(javaVersion, filteredBcp, extDirs, endorsedDirs);
    }

    private boolean isRunningOnJava9orLater() {
        if (isRunningOnJava9orLater == null) {
            try {
                Runtime.class.getDeclaredMethod("version", new Class[0]);
                isRunningOnJava9orLater = Boolean.TRUE;
            } catch (Throwable e) {
                isRunningOnJava9orLater = Boolean.FALSE;
            }
        }
        return isRunningOnJava9orLater;
    }

    private static String[] splitPath(String path) {
        if ("null".equals(path)) {
            return new String[0];
        } else {
            return path.split(Pattern.quote(File.pathSeparator));
        }
    }

    protected Logger getLog() {
        return log;
    }

    protected File getLibDetectorJar() throws ArtifactResolutionException {
        if (libDetectorJar != null) {
            return libDetectorJar;
        }

        MavenSession mavenSession = legacySupport.getSession();
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.eclipse.tycho");
        dependency.setArtifactId("tycho-lib-detector");
        dependency.setVersion(TYCHO_LIB_DETECTOR_VERSION);
        Artifact artifact = dependenciesResolver.resolveArtifact(mavenSession.getCurrentProject(), mavenSession,
                dependency);
        return libDetectorJar = artifact.getFile();
    }
}
