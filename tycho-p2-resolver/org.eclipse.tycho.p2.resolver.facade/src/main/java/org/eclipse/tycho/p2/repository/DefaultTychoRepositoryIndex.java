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
package org.eclipse.tycho.p2.repository;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of TychoRepositoryIndex defines tycho repository index format and provides
 * generic index read/write methods.
 */
public class DefaultTychoRepositoryIndex implements TychoRepositoryIndex {
    protected static final String ENCODING = "UTF8";

    protected static final String EOL = "\n";

    // must match extension point filter in org.eclipse.tycho.p2.maven.repository/plugin.xml
    public static final String INDEX_RELPATH = ".meta/p2-metadata.properties";

    protected Set<GAV> gavs = new LinkedHashSet<GAV>();

    protected DefaultTychoRepositoryIndex() {
    }

    public DefaultTychoRepositoryIndex(InputStream indexFileContent) throws IOException {
        gavs = read(indexFileContent);
    }

    protected static Set<GAV> read(InputStream is) throws IOException {
        LinkedHashSet<GAV> result = new LinkedHashSet<GAV>();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING));
        try {
            String str;
            while ((str = br.readLine()) != null) {
                result.add(GAV.parse(str));
            }
        } finally {
            br.close();
        }

        return result;
    }

    public List<GAV> getProjectGAVs() {
        return new ArrayList<GAV>(gavs);
    }

    public void addProject(String groupId, String artifactId, String version) {
        addProject(new GAV(groupId, artifactId, version));
    }

    public void addProject(GAV gav) {
        gavs.add(gav);
    }

    public void write(OutputStream os) throws IOException {
        Writer out = new OutputStreamWriter(new BufferedOutputStream(os), ENCODING);
        try {
            for (GAV gav : gavs) {
                out.write(gav.toExternalForm());
                out.write(EOL);
            }
            out.flush();
        } finally {
            out.close();
        }
    }

}
