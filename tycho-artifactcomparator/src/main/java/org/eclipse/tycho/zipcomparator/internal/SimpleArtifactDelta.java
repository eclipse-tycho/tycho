/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.zipcomparator.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.tycho.artifactcomparator.ArtifactDelta;

public class SimpleArtifactDelta implements ArtifactDelta {

    private final String message;
    private final String baseline;
    private final String reactor;
    private final String detailed;

    public SimpleArtifactDelta(String message) {
        this(message, null, null);
    }

    public SimpleArtifactDelta(String message, String baseline, String reactor) {
        this(message, message, baseline, reactor);
    }

    public SimpleArtifactDelta(String message, String detailed, String baseline, String reactor) {
        this.detailed = detailed;
        this.baseline = baseline;
        this.reactor = reactor;
        if (message == null) {
            throw new IllegalArgumentException();
        }
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDetailedMessage() {
        return detailed;
    }

    public String getBaseline() {
        return baseline;
    }

    public String getReactor() {
        return reactor;
    }

    @Override
    public void writeDetails(File destination) throws IOException {
        if (getBaseline() != null) {
            writeFile(destination.getParentFile(), destination.getName() + "-baseline", getBaseline());
        }
        if (getReactor() != null) {
            writeFile(destination.getParentFile(), destination.getName() + "-build", getReactor());
        }
    }

    protected static void writeFile(File basedir, String path, String data) throws IOException {
        File file = new File(basedir, path).getAbsoluteFile();
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), data);
    }

    protected static void writeFile(File basedir, String path, InputStream data) throws IOException {
        File file = new File(basedir, path).getAbsoluteFile();
        file.getParentFile().mkdirs();
        Files.copy(data, file.toPath());
    }
}
