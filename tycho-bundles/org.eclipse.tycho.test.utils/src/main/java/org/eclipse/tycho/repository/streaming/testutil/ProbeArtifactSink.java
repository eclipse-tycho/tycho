/*******************************************************************************
 * Copyright (c) 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Tobias Oberlies (SAP SE) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.streaming.testutil;

import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;

public class ProbeArtifactSink implements IArtifactSink {

    private IArtifactKey artifactKey;

    ProbeOutputStream sink = null;
    private boolean aborted = false;
    private boolean committed = false;

    public static ProbeArtifactSink newArtifactSinkFor(IArtifactKey artifactKey) {
        return new ProbeArtifactSink(artifactKey);
    }

    public ProbeArtifactSink(IArtifactKey artifactKey) {
        this.artifactKey = artifactKey;
    }

    @Override
    public IArtifactKey getArtifactToBeWritten() {
        return artifactKey;
    }

    @Override
    public boolean canBeginWrite() {
        return !committed;
    }

    @Override
    public OutputStream beginWrite() {
        if (committed) {
            fail("Can not call beginWrite() after commitWrite()");
        }
        aborted = false;
        sink = new ProbeOutputStream();
        return sink;
    }

    public boolean writeIsStarted() {
        return sink != null;
    }

    @Override
    public void abortWrite() {
        if (!writeIsStarted()) {
            throw new AssertionError("Should not call abortWrite() if beginWrite() wasn't called");
        }
        if (writeIsCommitted()) {
            throw new AssertionError("Can not call abortWrite() after commitWrite()");
        }

        aborted = true;
    }

    public boolean writeIsAborted() {
        return aborted;
    }

    @Override
    public void commitWrite() {
        if (!writeIsStarted()) {
            throw new AssertionError("Should not call abortWrite() if beginWrite() wasn't called");
        }
        if (writeIsAborted()) {
            throw new AssertionError("Can not call commitWrite() after abortWrite()");
        }

        committed = true;
    }

    public boolean writeIsCommitted() {
        return committed;
    }

    public int committedBytes() {
        if (!writeIsCommitted()) {
            return 0;
        }
        return sink.writtenBytes();
    }

    public Set<String> getFilesInZip() throws IOException {
        if (!writeIsCommitted()) {
            return Collections.emptySet();
        }
        return sink.getFilesInZip();
    }

    public void checkConsistencyWithStatus(IStatus status) {
        assertNotNull(status);

        if (writeIsCommitted()) {
            // status must be non-fatal
            assertThat(status, is(either(okStatus()).or(warningStatus())));
        }
        if (writeIsAborted()) {
            // status must be fatal
            assertThat(status, is(errorStatus()));
        }
        if (writeIsStarted()) {
            // if the sink was opened, it should also be closed
            assertThat(writeIsAborted() || writeIsCommitted(), is(true));
        }
    }

}
