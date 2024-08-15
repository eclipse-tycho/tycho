/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.tycho.model.manifest.MutableBundleManifest;

public class MutableBndFile {

    private final List<BndLine> bndLines = new ArrayList<BndLine>();

    public void setValue(String key, String value) {
        if (key == null) {
            return;
        }
        for (BndLine bndLine : bndLines) {
            if (Objects.equals(key, bndLine.key)) {
                bndLine.newValue = value;
                return;
            }
        }
    }

    public String getValue(String key) {
        if (key == null) {
            return null;
        }
        for (BndLine bndLine : bndLines) {
            if (Objects.equals(key, bndLine.key)) {
                if (bndLine.newValue != null) {
                    return bndLine.newValue;
                }
                return bndLine.value;
            }
        }
        return null;
    }

    public void write(File bndFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(bndFile.toPath())) {
            write(writer);
        }
    }

    public void write(Writer writer) throws IOException {
        for (BndLine bndLine : bndLines) {
            if (bndLine.newValue == null || bndLine.key == null) {
                writer.write(bndLine.collect());
            } else {
                String value = bndLine.value;
                if (value == null) {
                    writer.write(bndLine.collect() + bndLine.newValue);
                } else {
                    writer.write(bndLine.collect().replace(value, bndLine.newValue));
                }
            }
        }
    }

    public static MutableBndFile read(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return read(is);
        }
    }

    public static MutableBndFile read(InputStream is) throws IOException {
        PushbackReader pushbackReader = new PushbackReader(
                new BufferedReader(new InputStreamReader(new BufferedInputStream(is), StandardCharsets.UTF_8)), 1);
        MutableBndFile bndFile = new MutableBndFile();
        BndLine line;
        BndLine last = null;
        while ((line = readLine(pushbackReader)) != null) {
            if (last != null && last.isContinuation()) {
                last.nextline = line;
            } else {
                bndFile.bndLines.add(line);
            }
            last = line;
        }
        bndFile.bndLines.forEach(BndLine::parse);
        return bndFile;

    }

    private static BndLine readLine(PushbackReader reader) throws IOException {
        BndLine bndLine = new BndLine();
        String str = MutableBundleManifest.readLineWithLineEnding(reader, lineEnding -> bndLine.eol = lineEnding);
        if (str == null) {
            return null;
        }
        bndLine.rawstring = str;
        return bndLine;
    }

}
