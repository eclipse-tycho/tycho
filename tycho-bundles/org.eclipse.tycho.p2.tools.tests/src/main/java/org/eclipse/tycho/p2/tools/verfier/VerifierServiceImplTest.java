/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.verfier;

import static org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest.sourceRepos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.eclipse.tycho.core.facade.BuildOutputDirectory;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.verifier.VerifierServiceImpl;
import org.eclipse.tycho.test.util.MemoryLog;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("boxing")
public class VerifierServiceImplTest {

    private MemoryLog logger;
    private VerifierServiceImpl subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        subject = new VerifierServiceImpl();
        MavenContextImpl mavenContext = new MavenContextImpl();
        logger = new MemoryLog(false);
        mavenContext.setLogger(logger);
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
        assertTrue(remainingErrorText().contains("dummy.class"));
        assertTrue(remainingErrorText().contains("has been tampered"));
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
        return subject.verify(repositories.getMetadataRepositories().get(0), repositories.getArtifactRepositories()
                .get(0), new BuildOutputDirectory(tempFolder.getRoot()));
    }

}
