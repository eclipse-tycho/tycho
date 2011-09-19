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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class MutableBundleManifest {

    private List<ManifestAttribute> lines = new ArrayList<ManifestAttribute>();

    private String unparsed;

    public void add(ManifestAttribute line) {
        lines.add(line);
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
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF8"));

        MutableBundleManifest mf = new MutableBundleManifest();
        ManifestAttribute curr = null;

        String str;
        while ((str = br.readLine()) != null) {
            if (str.length() == 0) {
                break;
            }

            if (str.length() == 0) {
                break;
            } else if (str.charAt(0) == ' ') {
                if (curr == null) {
                    throw new IOException("");
                }
                curr.add(str.substring(1));
            } else {
                curr = new ManifestAttribute(str);
                mf.add(curr);
            }
        }

        if (str != null) {
            StringBuilder sb = new StringBuilder("\r\n");
            int ch;
            while ((ch = br.read()) != -1) {
                sb.append((char) ch);
            }
            mf.setUnparsed(sb.toString());
        }

        return mf;
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

        for (int i = 0; i < mf.lines.size(); i++) {
            if (i > 0) {
                w.write("\r\n");
            }
            mf.lines.get(i).writeTo(w);
        }
        w.write("\r\n");

        if (mf.unparsed != null) {
            w.write(mf.unparsed);
        }

        w.flush();
    }

    public String getSymbolicName() {
        ManifestElement[] id = parseHeader(Constants.BUNDLE_SYMBOLICNAME);
        return id[0].getValue();
    }

    public String getVersion() {
        ManifestElement[] version = parseHeader(Constants.BUNDLE_VERSION);
        return version[0].getValue();
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
        for (ManifestAttribute line : lines) {
            if (line.hasName(name)) {
                return line;
            }
        }
        return null;
    }

    public void setVersion(String version) {
        ManifestAttribute attr = getAttribute(Constants.BUNDLE_VERSION);
        if (attr != null) {
            attr.set(Constants.BUNDLE_VERSION, version);
        } else {
            lines.add(new ManifestAttribute(Constants.BUNDLE_VERSION, version));
        }
    }

}
