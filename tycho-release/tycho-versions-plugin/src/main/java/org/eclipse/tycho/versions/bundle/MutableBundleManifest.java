/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Nepomuk Seiler - set export-package attribute implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class MutableBundleManifest {

    private final List<ManifestAttribute> attributes = new ArrayList<ManifestAttribute>();

    private String lineEnding = "";
    private String unparsed;

    public void add(ManifestAttribute attribute) {
        attributes.add(attribute);
    }

    public static MutableBundleManifest read(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            return read(is);
        } finally {
            IOUtil.close(is);
        }
    }

    public static MutableBundleManifest read(InputStream is) throws IOException {
        PushbackReader br = new PushbackReader(new BufferedReader(new InputStreamReader(is, "UTF8")), 1);

        MutableBundleManifest mf = new MutableBundleManifest();
        ManifestAttribute curr = null;

        String str;
        while ((str = readLineWithLineEnding(br, mf)) != null) {
            if (str.trim().length() == 0) {
                break;
            } else if (str.charAt(0) == ' ') {
                if (curr == null) {
                    throw new IOException("");
                }
                curr.add(str);
            } else {
                curr = new ManifestAttribute(str);
                mf.add(curr);
            }
        }

        if (str != null) {
            StringBuilder sb = new StringBuilder(str);
            int ch;
            while ((ch = br.read()) != -1) {
                sb.append((char) ch);
            }
            mf.setUnparsed(sb.toString());
        }

        return mf;
    }

    private static String readLineWithLineEnding(PushbackReader reader, MutableBundleManifest mf) throws IOException {
        StringBuilder result = new StringBuilder();
        int ch, lastch = -1;

        while ((ch = reader.read()) != -1) {
            if (lastch == '\r') {
                if (ch == '\n') {
                    result.append((char) ch);
                    mf.setLineEndingWhenFirstLine("\r\n");
                } else {
                    reader.unread(ch);
                    mf.setLineEndingWhenFirstLine("\r");
                }
                break;
            }

            result.append((char) ch);

            if (ch == '\n' || ch == '\u2028' || ch == '\u2029' || ch == '\u0085') { // see Scanner#LINE_SEPARATOR_PATTERN
                mf.setLineEndingWhenFirstLine(new String(new char[] { (char) ch }));
                break;
            }

            lastch = ch;
        }

        if (result.length() > 0) {
            return result.toString();
        }
        return null;
    }

    private void setLineEndingWhenFirstLine(String lineEnding) {
        if (this.lineEnding.length() == 0 && lineEnding != null) {
            this.lineEnding = lineEnding;
        }
    }

    private void setUnparsed(String unparsed) {
        this.unparsed = unparsed;
    }

    public static void write(MutableBundleManifest mf, File file) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            write(mf, os);
        } finally {
            IOUtil.close(os);
        }
    }

    public static void write(MutableBundleManifest mf, OutputStream os) throws IOException {
        Writer w = new OutputStreamWriter(os, "UTF8");

        for (ManifestAttribute attribute : mf.attributes) {
            attribute.writeTo(w, mf.lineEnding);
        }

        if (mf.unparsed != null) {
            w.write(mf.unparsed);
        }

        w.flush();
    }

    public String getSymbolicName() {
        ManifestElement[] id = parseHeader(Constants.BUNDLE_SYMBOLICNAME);
        if (id != null && id.length > 0) {
            return id[0].getValue();
        }
        return null;
    }

    public String getVersion() {
        ManifestElement[] version = parseHeader(Constants.BUNDLE_VERSION);
        if (version != null && version.length > 0) {
            return version[0].getValue();
        }
        return null;
    }

    private ManifestElement[] parseHeader(String name) {
        ManifestAttribute property = getAttribute(name);
        if (property == null) {
            return null;
        }
        try {
            return ManifestElement.parseHeader(name, property.getValue());
        } catch (BundleException e) {
            throw new IllegalArgumentException("Could not parse bundle manifest", e);
        }
    }

    private ManifestAttribute getAttribute(String name) {
        for (ManifestAttribute attribute : attributes) {
            if (attribute.hasName(name)) {
                return attribute;
            }
        }
        return null;
    }

    public void setVersion(String version) {
        ManifestAttribute attr = getAttribute(Constants.BUNDLE_VERSION);
        if (attr != null) {
            attr.set(Constants.BUNDLE_VERSION, version);
        } else {
            attributes.add(new ManifestAttribute(Constants.BUNDLE_VERSION, version));
        }
    }

    public void setExportedPackageVersion(String version) {
        ManifestAttribute attr = getAttribute(Constants.EXPORT_PACKAGE);
        if (attr == null)
            return;

        String newExportedPackage = attr.getValue().replaceAll("(version=)\"(.*?)\"", "$1\"" + version + "\"") //Replacing all version fields
                .replaceAll(".qualifier", ""); // Removing all .qualifier
        attr.set(Constants.EXPORT_PACKAGE, newExportedPackage);
    }
}
