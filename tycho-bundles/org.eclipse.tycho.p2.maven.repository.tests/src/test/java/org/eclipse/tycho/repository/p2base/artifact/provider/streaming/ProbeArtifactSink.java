/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.p2base.artifact.provider.streaming;

import static org.eclipse.tycho.test.util.StatusMatchers.errorStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.okStatus;
import static org.eclipse.tycho.test.util.StatusMatchers.warningStatus;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.IArtifactSink;
import org.eclipse.tycho.repository.test.util.ProbeOutputStream;

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

    public IArtifactKey getArtifactToBeWritten() {
        return artifactKey;
    }

    public boolean canBeginWrite() {
        return !committed;
    }

    public OutputStream beginWrite() {
        if (committed == true) {
            fail("Can not call beginWrite() after commitWrite()");
        }
        aborted = false;
        sink = new ProbeOutputStream();
        return sink;
    }

    public boolean writeIsStarted() {
        return sink != null;
    }

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
        assertThat(status, is(notNullValue()));

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
            assertThat(writeIsAborted() || writeIsStarted(), is(true));
        }
    }

}
