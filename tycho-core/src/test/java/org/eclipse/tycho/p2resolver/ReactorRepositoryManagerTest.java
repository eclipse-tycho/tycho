/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to new API 
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.tycho.p2.target.facade.PomDependencyCollector;
import org.eclipse.tycho.repository.registry.facade.ReactorRepositoryManager;
import org.eclipse.tycho.test.util.ReactorProjectStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ReactorRepositoryManagerTest extends MavenServiceStubbingTestBase {

    private ReactorRepositoryManager subject;
    @TempDir
    Path tempManager;

    private PomDependencyCollector pomDependencyCollector;

    @BeforeEach
    public void setUpContext() throws Exception {
        pomDependencyCollector = new PomDependencyCollectorImpl(logVerifier.getLogger(),
                new ReactorProjectStub(newFolder("temp"), "test"), getProvisioningAgent());
    }

    @Test
    public void testReactorRepositoryManagerServiceAvailability() throws Exception {
        subject = container.lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }

    @Test
    public void testReactorRepositoryManagerFacadeServiceAvailability() throws Exception {
        subject = container.lookup(ReactorRepositoryManager.class);

        assertNotNull(subject);
    }


    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempManager.resolve(path)).toFile();
    }
}
