/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.p2.tools.verifier.VerifierServiceImpl;
import org.eclipse.tycho.p2.tools.verifier.facade.VerifierService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("boxing")
public class VerifierServiceImplTest {

    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("a", "b", "c"));

    private MemoryLog logger = new MemoryLog();
    private VerifierService subject = new VerifierServiceImpl();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testValidFileRepository() throws Exception {
        final RepositoryReferences repositories = sourceRepos("selfsigned");
        assertEquals(true, verify(repositories));
    }

    @Test
    public void testFileRepositoryWithWrongMd5Sum() throws Exception {
        final RepositoryReferences repositories = sourceRepos("wrong_checksum");
        assertEquals(false, verify(repositories));
        assertTrue(firstErrorLine().contains("jarsigning"));
        assertTrue(remainingErrorText().contains("md5 hash"));
    }

    @Test
    public void testFileRepositoryWithTamperedArtifact() throws Exception {
        final RepositoryReferences repositories = sourceRepos("tampered_file");
        assertEquals(false, verify(repositories));
        assertTrue(firstErrorLine().contains("osgi.bundle"));
        assertTrue(firstErrorLine().contains("jarsigning"));
        assertTrue(remainingErrorText().contains("dummy.class"));
        assertTrue(remainingErrorText().contains("has been tampered"));
    }

    private String remainingErrorText() {
        return logger.errors.subList(1, logger.errors.size()).toString().toLowerCase(Locale.ENGLISH);
    }

    private String firstErrorLine() {
        return logger.errors.get(0).toLowerCase(Locale.ENGLISH);
    }

    private boolean verify(final RepositoryReferences repositories) throws FacadeException {
        BuildContext context = new BuildContext(null, DEFAULT_ENVIRONMENTS, tempFolder.getRoot());

        return subject.verify(repositories.getMetadataRepositories().get(0), repositories.getArtifactRepositories()
                .get(0), context, logger);
    }

    static class MemoryLog implements MavenLogger {
        List<String> warnings = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();

        public void error(String message) {
            errors.add(message);
        }

        public void warn(String message) {
            warnings.add(message);
        }

        public void warn(String message, Throwable cause) {
            warnings.add(message);
        }

        public void info(String message) {
        }

        public void debug(String message) {
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isExtendedDebugEnabled() {
            return false;
        }
    }
}
