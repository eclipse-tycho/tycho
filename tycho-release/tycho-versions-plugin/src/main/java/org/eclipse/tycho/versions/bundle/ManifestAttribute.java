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
        lines.add(chopNewLine(str));
    }

    public ManifestAttribute(String name, String value) {
        set(name, value);
    }

    public void add(String str) {
        String choppedLine = chopNewLine(str);

        if (!choppedLine.substring(0, 1).startsWith(" ")) {
            throw new IllegalArgumentException("Additional attribute lines must start with a space.");
        }
        if (choppedLine.contains("\n") || choppedLine.contains("\r")) {
            throw new IllegalArgumentException("Additional attribute line must not consist of multiple lines");
        }

        lines.add(choppedLine);
    }

    private String chopNewLine(String str) {
        if (str.length() > 0) {
            char lastChar = str.charAt(str.length() - 1);
            if (lastChar == '\n' || lastChar == '\r' || lastChar == '\u2028' || lastChar == '\u2029'
                    || lastChar == '\u0085') // see Scanner#LINE_SEPARATOR_PATTERN
            {
                return str.substring(0, str.length() - (str.endsWith("\r\n") ? 2 : 1));
            }
        }
        return str;
    }

    /**
     * Writes the lines to {@code w} using the given line termination chars. There will be a
     * trailing newline!
     */
    public void writeTo(Writer w, String lineTermination) throws IOException {
        for (String line : lines) {
            w.write(line);
            w.write(lineTermination);
        }
    }

    public boolean hasName(String name) {
        return lines.get(0).startsWith(name + ": ");
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            sb.append(lines.get(i).substring(1));
        }

        int idx = sb.indexOf(": ");
        if (idx > 0) {
            return sb.substring(idx + 2);
        }

        return null;
    }

    public void set(String name, String value) {
        String attribute = (name != null ? name.trim() : "") + ": " + (value != null ? value.trim() : "");

        lines.clear();
        while (attribute.length() > 71) {
            lines.add(attribute.substring(0, 70));
            attribute = " " + attribute.substring(70);
        }
        lines.add(attribute);
    }

}
