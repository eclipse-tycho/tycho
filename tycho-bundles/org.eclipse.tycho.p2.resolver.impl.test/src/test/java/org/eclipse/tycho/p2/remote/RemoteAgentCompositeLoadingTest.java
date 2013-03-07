/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.impl.test.ResourceUtil;
import org.eclipse.tycho.test.util.LogVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RemoteAgentCompositeLoadingTest {

    @Rule
    public final TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public final LogVerifier logVerifier = new LogVerifier();

    private RemoteAgent subject;

    @Before
    public void initSubject() throws ProvisionException, Exception {
        File localRepo = tempManager.newFolder("localRepo");
        subject = new RemoteAgent(new MavenContextImpl(localRepo, logVerifier.getLogger()));
    }

    @Test
    public void testLoadingCompositeRepositoryWithMissingChildFailsByDefault() throws IOException {
        /*
         * In Tycho, we want composite repositories to fail if they have missing children (and don't
         * explicitly specify the "p2.atomic.composite.loading" property).
         */
        ProvisionException expectedException = null;
        try {
            subject.getService(IArtifactRepositoryManager.class).loadRepository(
                    ResourceUtil.resourceFile("repositories/composite/missingChildAndAtomicUnset").toURI(),
                    new NullProgressMonitor());
        } catch (ProvisionException e) {
            expectedException = e;
        }

        assertThat(expectedException, not(nullValue()));
        assertThat(expectedException.getStatus().getCode(), is(ProvisionException.REPOSITORY_FAILED_READ));
    }

}
