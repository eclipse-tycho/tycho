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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of TychoRepositoryIndex defines tycho repository index format and provides
 * generic index read/write methods.
 */
public abstract class DefaultTychoRepositoryIndex implements TychoRepositoryIndex {

    private static final String ENCODING = "UTF8";
    private static final String EOL = "\n";
    private Set<GAV> gavs;

    protected DefaultTychoRepositoryIndex() {
        this(Collections.<GAV> emptySet());
    }

    /**
     * @param initialContent
     *            must not contain <code>null</code>
     */
    public DefaultTychoRepositoryIndex(Set<GAV> initialContent) {
        gavs = new LinkedHashSet<GAV>(initialContent);
    }

    public Set<GAV> getProjectGAVs() {
        return Collections.unmodifiableSet(new LinkedHashSet<GAV>(gavs));
    }

    public void addGav(GAV gav) {
        if (gav == null)
            throw new NullPointerException();
        gavs.add(gav);
    }

    public void removeGav(GAV gav) {
        gavs.remove(gav);
    }

    protected void setGavs(Set<GAV> content) {
        this.gavs = content;
    }

    protected void write(OutputStream outStream) throws IOException {
        Writer out = new OutputStreamWriter(new BufferedOutputStream(outStream), ENCODING);
        try {
            for (GAV gav : getProjectGAVs()) {
                out.write(gav.toExternalForm());
                out.write(EOL);
            }
            out.flush();
        } finally {
            out.close();
        }
    }

    protected Set<GAV> read(InputStream inStream) throws IOException {
        LinkedHashSet<GAV> result = new LinkedHashSet<GAV>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, ENCODING));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                GAV parsedGAV = GAV.parse(line);
                if (parsedGAV != null) {
                    result.add(parsedGAV);
                }
            }
        } finally {
            reader.close();
        }
        return result;
    }

}
