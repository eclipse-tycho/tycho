/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.locking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.util.Arrays;

/**
 * Lock a file in a spawned JVM process and hold it for a certain time before exiting.
 */
public class LockProcess {

    private static final String LOCK_ACQUIRED_MSG = "##lock acquired##";
    private File lockMarkerFile;
    private long waitTime;
    private Process process;
    private File tmpClassDir;

    public LockProcess(File file, long waitTime) {
        this.lockMarkerFile = getTychoLockMarkerFile(file);
        this.waitTime = waitTime;
    }

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        long wait = Long.valueOf(args[1]);
        RandomAccessFile raFile = new RandomAccessFile(file, "rw");
        FileLock lock = raFile.getChannel().lock(0, 1, false);
        System.out.println(LOCK_ACQUIRED_MSG);
        Thread.sleep(wait);
        lock.release();
        raFile.close();
    }

    public void lockFileInForkedProcess() {
        copyClassFile();
        try {
            File javaExecutable = new File(System.getProperty("java.home"), "bin/java"
                    + (File.separatorChar == '\\' ? ".exe" : ""));
            String[] commandLine = new String[] { javaExecutable.getAbsolutePath(), "-cp",
                    tmpClassDir.getAbsolutePath(), LockProcess.class.getName(), lockMarkerFile.getAbsolutePath(),
                    String.valueOf(waitTime) };
            ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(commandLine));
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // wait until file lock is acquired
            while (!LOCK_ACQUIRED_MSG.equals(reader.readLine())) {
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanup() throws IOException, InterruptedException {
        if (process == null) {
            throw new IllegalStateException("process not started");
        }
        InputStream stream = process.getInputStream();
        while (stream.read() != -1) {
            // consume stream
        }
        process.waitFor();
        process = null;
        deleteRecursive(tmpClassDir);
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                deleteRecursive(subFile);
            }
        } else if (file.isFile()) {
            file.delete();
        }
    }

    private void copyClassFile() {
        InputStream in = LockProcess.class.getResourceAsStream(LockProcess.class.getSimpleName() + ".class");
        try {
            this.tmpClassDir = File.createTempFile("tmp", "classes");
            tmpClassDir.delete();
            tmpClassDir.mkdirs();
            String classNamePath = LockProcess.class.getName().replace('.', '/') + ".class";
            File tmpClassFile = new File(tmpClassDir, classNamePath);
            tmpClassFile.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(tmpClassFile);
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getTychoLockMarkerFile(File file) {
        return new File(file.getParentFile(), file.getName() + ".tycholock");
    }

}
