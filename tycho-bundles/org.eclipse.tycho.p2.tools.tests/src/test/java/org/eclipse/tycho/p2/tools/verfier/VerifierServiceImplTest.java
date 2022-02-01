/*******************************************************************************
 * Copyright (c) 2011, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.verfier;

import static org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest.sourceRepos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.MockMavenContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.verifier.VerifierServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VerifierServiceImplTest {

    private ErrorStoreMemoryLog logger;
    private VerifierServiceImpl subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        subject = new VerifierServiceImpl();
        logger = new ErrorStoreMemoryLog();
        MavenContext mavenContext = new MockMavenContext(null, logger);
        subject.setMavenContext(mavenContext);
    }

    @Test
    public void testValidFileRepository() throws Exception {
        final RepositoryReferences repositories = sourceRepos("selfsigned");
        assertEquals(true, verify(repositories));
    }

    @Test
    public void testFileRepositoryWithWrongMd5Sum() throws Exception {
        final RepositoryReferences repositories = sourceRepos("invalid/wrong_checksum");
        assertEquals(false, verify(repositories));
        // we expect two errors to be reported: the feature "jarsinging.feature" has a wrong md5 hash and
        // the osgi.bundle "jarsigning" has also a wrong md5 hash.
        // As each error is reported on two lines, we have 4 lines of error messages:
        assertEquals(logger.errors.size(), 4);
        // The first and third line contain the names of the units in error:
        String unitsInError = logger.errors.get(0) + logger.errors.get(2);
        assertTrue(unitsInError.contains("jarsigning.feature") && unitsInError.contains("osgi.bundle"));
        // The second and fourth line contain the type of error: md5 hash
        assertTrue(logger.errors.get(1).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
        assertTrue(logger.errors.get(3).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
    }

    @Test
    public void testFileRepositoryWithTamperedArtifact() throws Exception {
        final RepositoryReferences repositories = sourceRepos("invalid/tampered_file");
        assertEquals(false, verify(repositories));
        assertTrue(firstErrorLine().contains("osgi.bundle"));
        assertTrue(firstErrorLine().contains("jarsigning"));
        assertTrue(remainingErrorText().contains("invalid content:dummy.class"));
    }

    @Test
    public void testMissingArtifactsReferencedInMetadata() throws Exception {
        final RepositoryReferences repositories = sourceRepos("invalid/missing_artifacts");
        assertEquals(false, verify(repositories));
        assertTrue(firstErrorLine().contains("osgi.bundle"));
        assertTrue(firstErrorLine().contains("tycho551.bundle1"));
        assertTrue(firstErrorLine().contains("missing"));
    }

    private String remainingErrorText() {
        return logger.errors.subList(1, logger.errors.size()).toString().toLowerCase(Locale.ENGLISH);
    }

    private String firstErrorLine() {
        return logger.errors.get(0).toLowerCase(Locale.ENGLISH);
    }

    private boolean verify(final RepositoryReferences repositories) throws FacadeException {
        return subject.verify(repositories.getMetadataRepositories().get(0),
                repositories.getArtifactRepositories().get(0), new BuildOutputDirectory(tempFolder.getRoot()));
    }

    class ErrorStoreMemoryLog implements MavenLogger {
        List<String> errors = new ArrayList<>();

        @Override
        public void error(String message) {
            errors.add(message);
        }

        @Override
        public void error(String message, Throwable cause) {
            error(message);
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable cause) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public boolean isExtendedDebugEnabled() {
            return true;
        }

        @Override
        public void debug(String message, Throwable cause) {

        }
    }

}
