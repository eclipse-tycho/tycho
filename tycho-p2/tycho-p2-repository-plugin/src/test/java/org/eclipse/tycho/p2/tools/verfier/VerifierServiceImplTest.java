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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.VerifierService;
import org.eclipse.tycho.core.VerifierServiceImpl;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VerifierServiceImplTest extends TychoPlexusTestCase {

    private ErrorStoreMemoryLog logger;
    private VerifierService subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        subject = lookup(VerifierService.class);
        logger = new ErrorStoreMemoryLog();
        VerifierServiceImpl impl = (VerifierServiceImpl) subject;
        impl.setLogger(logger);
    }

    @Test
    public void testValidFileRepository() throws Exception {
        final RepositoryReferences repositories = sourceRepos("selfsigned");
        assertEquals(true, verify(repositories));
    }

    @Test
    public void testFileRepositoryWithWrongMd5Sum() throws Exception {
        final RepositoryReferences repositories = sourceRepos("wrong_checksum");
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
        final RepositoryReferences repositories = sourceRepos("tampered_file");
        assertEquals(false, verify(repositories));
        assertTrue(firstErrorLine().contains("osgi.bundle"));
        assertTrue(firstErrorLine().contains("jarsigning"));
        assertTrue(remainingErrorText().contains("invalid content:dummy.class"));
    }

    @Test
    public void testMissingArtifactsReferencedInMetadata() throws Exception {
        final RepositoryReferences repositories = sourceRepos("missing_artifacts");
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

    class ErrorStoreMemoryLog extends AbstractLogger {
        public ErrorStoreMemoryLog() {
            super(LEVEL_DEBUG, "TestLogger");
        }

        List<String> errors = new ArrayList<>();

        @Override
        public void debug(String message, Throwable throwable) {

        }

        @Override
        public void info(String message, Throwable throwable) {

        }

        @Override
        public void warn(String message, Throwable throwable) {

        }

        @Override
        public void error(String message, Throwable throwable) {
            errors.add(message);

        }

        @Override
        public void fatalError(String message, Throwable throwable) {

        }

        @Override
        public Logger getChildLogger(String name) {
            return null;
        }
    }

    public static RepositoryReferences sourceRepos(String... repoIds) throws IOException {
        RepositoryReferences result = new RepositoryReferences();
        for (String repoId : repoIds) {
            result.addMetadataRepository(resourceFile("repositories/" + repoId));
            result.addArtifactRepository(resourceFile("repositories/" + repoId));
        }
        return result;
    }

}
