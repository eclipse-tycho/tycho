/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ManifestAttribute {
    private List<String> lines = new ArrayList<String>();

    public ManifestAttribute(String str) {
        lines.add(str);
    }

    public ManifestAttribute(String name, String value) {
        set(name, value);
    }

    public void add(String str) {
        lines.add(str);
    }

    public void writeTo(Writer w) throws IOException {
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                w.write("\r\n ");
            }
            w.write(lines.get(i));
        }
    }

    public boolean hasName(String name) {
        if (lines.size() > 0) {
            return lines.get(0).startsWith(name + ": ");
        }
        return false;
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }

        int idx = sb.indexOf(": ");
        if (idx > 0) {
            return sb.substring(idx + 2);
        }

        return null;
    }

    public void set(String name, String value) {
        lines.clear();
        StringBuilder sb = new StringBuilder(name + ": " + value);
        for (int i = 71; i < sb.length(); i += 73) {
            sb.insert(i, "\r\n ");
        }
        lines.add(sb.toString());
    }

}
