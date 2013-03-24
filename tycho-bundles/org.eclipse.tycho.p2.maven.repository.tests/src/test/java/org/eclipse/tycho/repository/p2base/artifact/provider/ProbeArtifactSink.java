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
package org.eclipse.tycho.repository.p2base.artifact.provider;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
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
        if (committed) {
            throw new AssertionError("Can not call abortWrite() after commitWrite()");
        }
        aborted = true;
    }

    public boolean writeIsAborted() {
        return aborted;
    }

    public void commitWrite() {
        if (aborted) {
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

}
