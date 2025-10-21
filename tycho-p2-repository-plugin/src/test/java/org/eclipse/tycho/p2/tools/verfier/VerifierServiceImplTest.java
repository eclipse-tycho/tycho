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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.VerifierService;
import org.eclipse.tycho.core.VerifierServiceImpl;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@PlexusTest
public class VerifierServiceImplTest {

    private ErrorStoreMemoryLog logger;

    @Inject
    private VerifierService subject;

    @TempDir
    File tempFolder;

    @BeforeEach
    public void setup() throws Exception {
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
        assertEquals(4, logger.errors.size());
        // The first and third line contain the names of the units in error:
        String unitsInError = logger.errors.get(0) + logger.errors.get(2);
        assertTrue(unitsInError.contains("jarsigning.feature") && unitsInError.contains("osgi.bundle"));
        // The second and fourth line contain the type of error: md5 hash
        assertTrue(logger.errors.get(1).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
        assertTrue(logger.errors.get(3).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
    }

    @Test
    @Disabled("As of Java 17.0.5 this test do not work anymore due to disabled signature algorithms, the repository is always assumed valid because the artifacts are considered effectively unsigned")
    public void testFileRepositoryWithTamperedArtifact() throws Exception {
        final RepositoryReferences repositories = sourceRepos("tampered_file");
        boolean verify = verify(repositories);
        assertEquals(false, verify,
                "The repository should not verify! (messages below)" + System.lineSeparator()
                        + logger.messages.stream().collect(Collectors.joining(System.lineSeparator())) + ")");
        assertContains(firstErrorLine(), "osgi.bundle");
        assertContains(firstErrorLine(), "jarsigning");
        assertContains(remainingErrorText(), "invalid content:dummy.class");
    }

    private void assertContains(String line, String string) {
        if (!line.contains(string)) {
            fail("Line '" + line + "' does not contain expected string '" + string + "'");
        }

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
                repositories.getArtifactRepositories().get(0), new BuildOutputDirectory(tempFolder));
    }

    class ErrorStoreMemoryLog extends AbstractLogger {
        public ErrorStoreMemoryLog() {
            super(LEVEL_DEBUG, "TestLogger");
        }

        List<String> errors = new ArrayList<>();

        List<String> messages = new ArrayList<>();

        @Override
        public void debug(String message, Throwable throwable) {
            messages.add(message);
        }

        @Override
        public void info(String message, Throwable throwable) {
            messages.add(message);
        }

        @Override
        public void warn(String message, Throwable throwable) {
            messages.add(message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            messages.add(message);
            errors.add(message);

        }

        @Override
        public void fatalError(String message, Throwable throwable) {
            messages.add(message);
        }

        @Override
        public Logger getChildLogger(String name) {
            return null;
        }
    }

    public static RepositoryReferences sourceRepos(String... repoIds) throws IOException {
        RepositoryReferences result = new RepositoryReferences();
        for (String repoId : repoIds) {
            File repoFile = resourceFile("repositories/" + repoId);
            result.addMetadataRepository(repoFile);
            result.addArtifactRepository(repoFile);
        }
        return result;
    }

    private static File resourceFile(String path) {
        File resolvedFile = new File("src/test/resources", path).getAbsoluteFile();
        if (!resolvedFile.canRead()) {
            throw new IllegalStateException(
                    "Test resource \"" + path + "\" not found under \"src/test/resources\" in the project");
        }
        return resolvedFile;
    }

}
