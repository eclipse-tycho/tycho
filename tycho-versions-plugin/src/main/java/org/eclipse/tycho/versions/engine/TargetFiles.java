/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import eu.maveniverse.domtrip.Document;

public class TargetFiles {

    private Map<File, Document> targets = new ConcurrentHashMap<File, Document>();

    public void addTargetFile(File targetFile) throws IOException {
        targets.put(targetFile, Document.of(targetFile.toPath()));
    }

    public Map<File, Document> getTargets() {
        return targets;
    }

    public void write(File targetFile) throws IOException {
        Document document = targets.get(targetFile);
        if (document == null) {
            return;
        }
        writeTarget(targetFile, document);
    }

    private void writeTarget(File targetFile, Document document)
            throws IOException, FileNotFoundException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            document.toXml(os);
        }
    }

    public void write() throws IOException {
        for (Entry<File, Document> e : targets.entrySet()) {
            writeTarget(e.getKey(), e.getValue());
        }
    }
}
