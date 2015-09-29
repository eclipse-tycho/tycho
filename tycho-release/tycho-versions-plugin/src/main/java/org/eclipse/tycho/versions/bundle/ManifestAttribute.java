/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - add setter with {@link MutableManifestElement}
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.osgi.framework.BundleException;

public class ManifestAttribute {
    private static final String ELEMENT_SEPARATOR = ",\n ";
    // content holds all lines that belong to this header, but are normalized to line endings with '\n'
    private final StringBuilder content = new StringBuilder();

    public ManifestAttribute(String str) {
        content.append(chopNewLine(str));
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

        content.append("\n");
        content.append(choppedLine);
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
        for (String line : content.toString().split("\n")) {
            w.write(line);
            w.write(lineTermination);
        }
    }

    public boolean hasName(String name) {
        return content.toString().startsWith(name + ": ");
    }

    public String getValue() {
        if (content.toString().indexOf(": ") > 0) {
            return content.substring(content.toString().indexOf(": ") + 2).replaceAll("\n ", "");
        }
        return null;
    }

    public void set(String name, List<MutableManifestElement> manifestElements) {
        content.setLength(0);
        content.append(name);
        content.append(": ");
        for (MutableManifestElement element : manifestElements) {
            content.append(element.toString());
            content.append(ELEMENT_SEPARATOR);
        }
        content.setLength(content.length() - ELEMENT_SEPARATOR.length());
    }

    public void set(String name, String value) {
        try {
            set(name, MutableManifestElement.parseHeader(name, value));
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

}
