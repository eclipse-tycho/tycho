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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.VerifierService;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.testing.TychoPlexusTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

public class VerifierServiceImplTest extends TychoPlexusTestCase {

    private VerifierService subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final List<String> loggerErrors = new ArrayList<>();
    private final List<String> loggerMessages = new ArrayList<>();

    @Before
    public void setup() throws Exception {
        subject = lookup(VerifierService.class);
        setLogger(subject, createLogger());
    }

    private void setLogger(Object instance, Logger logger) {
        try {
            Field loggerField = instance.getClass().getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(instance, logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Logger createLogger() {
        return (Logger) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ Logger.class },
                (proxy, method, args) -> {
                    String message = String.valueOf(args[0]);
                    loggerMessages.add(message);
                    if (method.getName().equals("error")) {
                        loggerErrors.add(message);
                    }
                    return null;
                });
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
        assertEquals(loggerErrors.size(), 4);
        // The first and third line contain the names of the units in error:
        String unitsInError = loggerErrors.get(0) + loggerErrors.get(2);
        assertTrue(unitsInError.contains("jarsigning.feature") && unitsInError.contains("osgi.bundle"));
        // The second and fourth line contain the type of error: md5 hash
        assertTrue(loggerErrors.get(1).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
        assertTrue(loggerErrors.get(3).toLowerCase(Locale.ENGLISH).contains("md5 hash"));
    }

    @Test
    @Ignore("As of Java 17.0.5 this test do not work anymore due to disabled signature algorithms, the repository is always assumed valid because the artifacts are considered effectively unsigned")
    public void testFileRepositoryWithTamperedArtifact() throws Exception {
        final RepositoryReferences repositories = sourceRepos("tampered_file");
        boolean verify = verify(repositories);
        assertEquals(
                "The repository should not verify! (messages below)" + System.lineSeparator()
                        + loggerMessages.stream().collect(Collectors.joining(System.lineSeparator())) + ")",
                false, verify);
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
        return loggerErrors.subList(1, loggerErrors.size()).toString().toLowerCase(Locale.ENGLISH);
    }

    private String firstErrorLine() {
        return loggerErrors.get(0).toLowerCase(Locale.ENGLISH);
    }

    private boolean verify(final RepositoryReferences repositories) throws FacadeException {
        return subject.verify(repositories.getMetadataRepositories().get(0),
                repositories.getArtifactRepositories().get(0), new BuildOutputDirectory(tempFolder.getRoot()));
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
