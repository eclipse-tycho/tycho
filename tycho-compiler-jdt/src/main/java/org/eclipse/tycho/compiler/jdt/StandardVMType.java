package org.eclipse.tycho.compiler.jdt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.tycho.compiler.jdt.copied.LibraryInfo;

public class StandardVMType {

    private File javaHome;
    private LibraryInfo libraryInfo;
    private File libDetectorJar;

    public StandardVMType(String javaHome, File libDetectorJar) {
        this.javaHome = new File(javaHome);
        this.libDetectorJar = libDetectorJar;
        this.libraryInfo = generateLibraryInfo();
    }

    private LibraryInfo generateLibraryInfo() {
        String executable = javaHome + File.separator + "bin" + File.separator + "java";
        if (File.separatorChar == '\\') {
            executable = executable + ".exe";
        }
        CommandLine cli = new CommandLine(executable);
        cli.addArguments(new String[] { "-classpath", libDetectorJar.getAbsolutePath(),
                "org.eclipse.tycho.libdetector.LibraryDetector" }, false);
        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(30 * 1000L);
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
            return parseLibraryInfo(outputStream.toString());
        } else {
            throw new RuntimeException(
                    cli.toString() + " process exit code was " + exitValue + ". Output: " + outputStream.toString());
        }

    }

    private LibraryInfo parseLibraryInfo(String output) {
        String[] parts = output.split(Pattern.quote("|"));
        if (parts.length != 4) {
            throw new IllegalStateException("Could not parse process output: " + output);
        }
        String javaVersion = parts[0];
        String[] bootclasspath = splitPath(parts[1]);
        String[] extDirs = splitPath(parts[2]);
        String[] endorsedDirs = splitPath(parts[3]);
        // workaround for missing bootclasspath in Java 9 - see JDT bug http://eclip.se/489207  
        if (CompilerOptions.versionToJdkLevel(javaVersion) >= ClassFileConstants.JDK9) {
            File jrtFsJar = new File(javaHome, "lib/jrt-fs.jar");
            if (!jrtFsJar.isFile()) {
                jrtFsJar = new File(javaHome, "jrt-fs.jar");
                if (!jrtFsJar.isFile()) {
                    throw new IllegalStateException("jrt-fs.jar not found in " + javaHome);
                }
            }
            bootclasspath = new String[] { jrtFsJar.getAbsolutePath() };
        }
        return new LibraryInfo(javaVersion, bootclasspath, extDirs, endorsedDirs);
    }

    private static String[] splitPath(String path) {
        if ("null".equals(path)) {
            return new String[0];
        } else {
            return path.split(Pattern.quote(File.pathSeparator));
        }
    }

    public LibraryInfo getLibraryInfo() {
        return this.libraryInfo;
    }

}
