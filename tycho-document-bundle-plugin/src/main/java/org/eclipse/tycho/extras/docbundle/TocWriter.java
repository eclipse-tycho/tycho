/*******************************************************************************
 * Copyright (c) 2013, 2014 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

public class TocWriter {
    private TocOptions options;

    private File javadocDir;

    private File basedir;

    private Log log;

    public TocWriter() {
    }

    public void setLog(final Log log) {
        this.log = log;
    }

    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    public void setJavadocDir(final File javadocDir) {
        this.javadocDir = javadocDir;
    }

    public void setOptions(final TocOptions options) {
        this.options = options;
        if (this.options == null) {
            this.options = new TocOptions();
        }
    }

    public void writeTo(final File tocFile) throws MojoExecutionException {
        if (tocFile == null) {
            return;
        }

        try {
            process(tocFile);
        } catch (final Exception e) {
            throw new MojoExecutionException("Failed to generate toc file", e);
        }
    }

    private void process(final File tocFile) throws Exception {

        tocFile.getParentFile().mkdirs();

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document doc = db.newDocument();

        fillDocument(doc, tocFile);

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        final DOMSource source = new DOMSource(doc);
        final StreamResult result = new StreamResult(tocFile);

        transformer.transform(source, result);
    }

    private void fillDocument(final Document doc, final File tocFile) throws Exception {
        final ProcessingInstruction pi = doc.createProcessingInstruction("NLS", "TYPE=\"org.eclipse.help.toc\"");
        doc.appendChild(pi);

        final Element toc = doc.createElement("toc");
        doc.appendChild(toc);
        toc.setAttribute("label", this.options.getMainLabel());

        final Element main = createTopic(doc, toc, this.options.getMainLabel(), this.options.getMainFilename());
        final Element packages = createTopic(doc, main, "Packages", null);
        createTopic(doc, main, "Constant Values", "constant-values.html");
        createTopic(doc, main, "Deprecated List", "deprecated-list.html");

        final LineNumberReader reader = new LineNumberReader(new FileReader(new File(this.javadocDir, "package-list")));
        try {
            String line;

            while ((line = reader.readLine()) != null) {
                createTopic(doc, packages, line, line.replace('.', '/') + "/package-summary.html");
            }
        } finally {
            reader.close();
        }
    }

    private Element createTopic(final Document doc, final Element parent, final String label, final String fileName)
            throws DOMException, IOException {
        final Element topic = doc.createElement("topic");
        parent.appendChild(topic);
        topic.setAttribute("label", label);
        if (fileName != null) {
            topic.setAttribute("href", makeRelative(this.basedir, new File(this.javadocDir.toString(), fileName)));
        }
        return topic;
    }

    private String makeRelative(final File base, final File file) throws IOException {
        final String result = base.getCanonicalFile().toURI().relativize(file.getCanonicalFile().toURI()).getPath();
        this.log.info(String.format("Make relative - base: %s, file: %s -> %s", base, file, result));
        return result;
    }
}
