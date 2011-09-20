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

package org.eclipse.tycho.p2.impl.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.eclipse.core.runtime.internal.adaptor.BasicLocation;
import org.eclipse.tycho.locking.facade.FileLockService;
import org.eclipse.tycho.locking.facade.FileLocker;
import org.eclipse.tycho.locking.facade.LockTimeoutException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileLockServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private FileLockService subject;

    @Before
    public void setup() {
        subject = new FileLockServiceImpl();
        ((FileLockServiceImpl) subject).setLocation(new BasicLocation(null, null, false, null));
    }

    @Test
    public void testIsLocked() throws IOException {
        FileLocker fileLocker = subject.getFileLocker(newTestFile());
        assertFalse(fileLocker.isLocked());
        fileLocker.lock();
        try {
            assertTrue(fileLocker.isLocked());
        } finally {
            fileLocker.release();
            assertFalse(fileLocker.isLocked());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeTimeout() throws IOException {
        FileLocker fileLocker = subject.getFileLocker(newTestFile());
        fileLocker.lock(-1L);
    }

    @Test
    public void testLockDirectory() throws IOException {
        File testDir = tempFolder.newFolder("test");
        FileLockerImpl fileLocker = (FileLockerImpl) subject.getFileLocker(testDir);
        fileLocker.lock();
        try {
            assertTrue(fileLocker.isLocked());
            assertEquals(new File(testDir, ".tycholock").getCanonicalPath(),
                    fileLocker.lockMarkerFile.getCanonicalPath());
        } finally {
            fileLocker.release();
        }
    }

    @Test
    public void testLockReentranceSameLocker() throws IOException {
        FileLocker fileLocker = subject.getFileLocker(newTestFile());
        fileLocker.lock();
        try {
            // locks are not re-entrant
            fileLocker.lock(0L);
            fail("lock already held by same VM but could be acquired a second time");
        } catch (LockTimeoutException e) {
            // expected
        } finally {
            fileLocker.release();
        }
    }

    @Test
    public void testReuseLockerObject() throws IOException {
        FileLocker fileLocker = subject.getFileLocker(newTestFile());
        lockAndRelease(fileLocker);
        lockAndRelease(fileLocker);
    }

    private void lockAndRelease(FileLocker fileLocker) {
        assertFalse(fileLocker.isLocked());
        fileLocker.lock();
        assertTrue(fileLocker.isLocked());
        fileLocker.release();
        assertFalse(fileLocker.isLocked());
    }

    @Test
    public void testLockReentranceDifferentLocker() throws IOException {
        final File testFile = newTestFile();
        FileLocker fileLocker1 = subject.getFileLocker(testFile);
        FileLocker fileLocker2 = subject.getFileLocker(testFile);
        // same file but different locker objects
        assertNotSame(fileLocker1, fileLocker2);
        fileLocker1.lock();
        try {
            fileLocker2.lock(0L);
            fail("lock already held by same VM but could be acquired a second time");
        } catch (LockTimeoutException e) {
            // expected
        } finally {
            fileLocker1.release();
        }
    }

    @Test
    public void testLockedByOtherProcess() throws Exception {
        File testFile = newTestFile();
        FileLocker locker = subject.getFileLocker(testFile);
        LockProcess lockProcess = new LockProcess(testFile, 200L);
        lockProcess.lockFileInForkedProcess();
        assertTrue(locker.isLocked());
        try {
            locker.lock(0L);
            fail("lock already held by other VM but could be acquired a second time");
        } catch (LockTimeoutException e) {
            // expected
        }
        lockProcess.cleanup();
    }

    @Test
    public void testTimeout() throws Exception {
        File testFile = newTestFile();
        FileLocker locker = subject.getFileLocker(testFile);
        long waitTime = 1000L;
        LockProcess lockProcess = new LockProcess(testFile, waitTime);
        long start = System.currentTimeMillis();
        lockProcess.lockFileInForkedProcess();
        locker.lock(20000L);
        try {
            long duration = System.currentTimeMillis() - start;
            assertTrue(duration >= waitTime);
        } finally {
            lockProcess.cleanup();
            locker.release();
        }
    }

    @Test
    public void testRelease() throws Exception {
        FileLocker locker = subject.getFileLocker(newTestFile());
        assertFalse(locker.isLocked());
        // releasing without holding the lock should do nothing
        locker.release();
    }

    @Test
    public void testMarkerFileDeletion() throws Exception {
        FileLockerImpl locker = (FileLockerImpl) subject.getFileLocker(newTestFile());
        locker.lock();
        assertTrue(locker.lockMarkerFile.isFile());
        locker.release();
        assertFalse(locker.lockMarkerFile.isFile());
    }

    @Test
    public void testURLEncoding() throws IOException {
        File testFile = new File(tempFolder.getRoot(), "file with spaces" + new Random().nextInt());
        File markerFile = new File(testFile.getAbsolutePath() + ".tycholock");
        FileLocker fileLocker = subject.getFileLocker(testFile);
        assertFalse(markerFile.isFile());
        fileLocker.lock();
        try {
            assertTrue(markerFile.isFile());
        } finally {
            fileLocker.release();
        }
    }

    private File newTestFile() throws IOException {
        File testFile = tempFolder.newFile("testfile-" + new Random().nextInt());
        return testFile;
    }

}
