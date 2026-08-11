/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.eclipse.tycho.test.util.TychoPlexusExtension;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TychoPlexusExtension.class)
public class RemoteAgentCompositeLoadingTest {

    @Inject
    protected PlexusContainer container;

    @TempDir
    Path tempManager;
    @RegisterExtension
    public final LogVerifier logVerifier = new LogVerifier();

    private IProvisioningAgent subject;

    @BeforeEach
    public void initSubject() throws Exception {
        newFolder("localRepo");
        subject = container.lookup(IProvisioningAgent.class);
    }

    @Test
    public void testLoadingCompositeRepositoryWithMissingChildFailsByDefault() throws IOException {
        /*
         * In Tycho, we want composite repositories to fail if they have missing children (and don't
         * explicitly specify the "p2.atomic.composite.loading" property).
         */
        ProvisionException e = assertThrows(ProvisionException.class,
                () -> subject.getService(IArtifactRepositoryManager.class).loadRepository(
                        ResourceUtil.resourceFile("repositories/composite/missingChildAndAtomicUnset").toURI(),
                        new NullProgressMonitor()));
        assertEquals(ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempManager.resolve(path)).toFile();
    }
}
