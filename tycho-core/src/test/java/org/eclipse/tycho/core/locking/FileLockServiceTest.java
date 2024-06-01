/*******************************************************************************
 * Copyright (c) 2011, 2023 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.eclipse.tycho.LockTimeoutException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileLockServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private FileLockServiceImpl subject;

    @Before
    public void setup() {
        subject = new FileLockServiceImpl();
    }

    @Test
    public void testIsLocked() throws IOException {
        File file = newTestFile();
        try (var locked = subject.lock(file)) {
            assertTrue(isLocked(file));
        }
        assertFalse(isLocked(file));
    }

    @Test
    public void testNegativeTimeout() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> subject.lock(newTestFile(), -1L));
    }

    @Test
    public void testLockDirectory() throws IOException {
        File testDir = tempFolder.newFolder("test");
        Path lockFile = getLockMarkerFile(testDir);
        try (var locked = subject.lock(testDir)) {
            assertTrue(isLocked(testDir));
            assertEquals(new File(testDir, ".tycholock").getCanonicalPath(), lockFile.toRealPath().toString());
        }
    }

    @Test
    public void testReuseLockerObject() throws IOException {
        File file = newTestFile();
        lockAndRelease(file);
        lockAndRelease(file);
    }

    private void lockAndRelease(File file) throws IOException {
        assertFalse(isLocked(file));
        try (var locked = subject.lock(file)) {
            assertTrue(isLocked(file));
        }
        assertFalse(isLocked(file));
    }

    @Test
    public void testLockReentranceDifferentLocker() throws IOException {
        final File testFile = newTestFile();
        assertThrows("lock already held by same VM but could be acquired a second time", LockTimeoutException.class,
                () -> {
                    subject.lock(testFile);
                    subject.lock(testFile, 0L);
                });
    }

    @Test
    public void testLockedByOtherProcess() throws Exception {
        File testFile = newTestFile();
        LockProcess lockProcess = new LockProcess(testFile, 200L);
        lockProcess.lockFileInForkedProcess();
        assertThrows("lock already held by other VM but could be acquired a second time", LockTimeoutException.class,
                () -> subject.lock(testFile, 0L));
        lockProcess.cleanup();
    }

    @Test
    public void testTimeout() throws Exception {
        File testFile = newTestFile();
        long waitTime = 1000L;
        LockProcess lockProcess = new LockProcess(testFile, waitTime);
        long start = System.currentTimeMillis();
        lockProcess.lockFileInForkedProcess();
        try (var locked = subject.lock(testFile, 20000L)) {
            long duration = System.currentTimeMillis() - start;
            assertTrue(duration >= waitTime);
        } finally {
            lockProcess.cleanup();
        }
    }

    @Test
    public void testMarkerFileDeletion() throws Exception {
        File file = newTestFile();
        Path lockFile = getLockMarkerFile(file);
        try (var locked = subject.lock(file)) {
            assertTrue(Files.isRegularFile(lockFile));
        }
        assertFalse(Files.isRegularFile(lockFile));
    }

    @Test
    public void testURLEncoding() throws IOException {
        File testFile = new File(tempFolder.getRoot(), "file with spaces" + new Random().nextInt());
        File markerFile = new File(testFile.getAbsolutePath() + ".tycholock");
        assertFalse(markerFile.isFile());
        try (var locked = subject.lock(testFile)) {
            assertTrue(markerFile.isFile());
        }
    }

    private File newTestFile() throws IOException {
        File testFile = tempFolder.newFile("testfile-" + new Random().nextInt());
        return testFile;
    }

    private Path getLockMarkerFile(File file) {
        return subject.getFileLocker(file.toPath()).fileLocker().lockMarkerFile;
    }

    private boolean isLocked(File file) throws IOException {
        try (var channel = FileChannel.open(getLockMarkerFile(file), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE); FileLock lock = channel.tryLock();) {
            return lock == null;
        } catch (OverlappingFileLockException e) {
            return true;
        }
    }

}
