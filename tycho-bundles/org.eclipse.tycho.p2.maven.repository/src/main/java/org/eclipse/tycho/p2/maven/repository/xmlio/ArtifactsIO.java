/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.xmlio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.p2.maven.repository.xmlio35.SimpleArtifactRepositoryIO;
import org.eclipse.tycho.repository.util.internal.BundleConstants;
import org.osgi.framework.BundleContext;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@SuppressWarnings("restriction")
public class ArtifactsIO {

    private static class Writer35M7 extends SimpleArtifactRepositoryIO.Writer {

        public Writer35M7(OutputStream output) throws IOException {
            super(output);
        }

        public void write(Set<? extends IArtifactDescriptor> descriptors) {
            writeArtifacts(descriptors);
            flush();
        }

    }

    private static class Parser35M7 extends SimpleArtifactRepositoryIO.Parser {

        private Set<IArtifactDescriptor> artifacts;

        public Parser35M7(BundleContext context, String bundleId) {
            super(context, bundleId);
        }

        @Override
        public synchronized void parse(InputStream stream) throws IOException {
            this.status = null;
            try {
                // TODO: currently not caching the parser since we make no assumptions
                // or restrictions on concurrent parsing
                XMLReader xmlReader = getParser().getXMLReader();
                ArtifactsHandler artifactsHandler = new ArtifactsHandler();
                xmlReader.setContentHandler(new RepositoryDocHandler(ARTIFACTS_ELEMENT, artifactsHandler));
                xmlReader.parse(new InputSource(stream));
                if (isValidXML()) {
                    artifacts = artifactsHandler.getArtifacts();
                }
            } catch (SAXException e) {
                throw new IOException(e.getMessage());
            } catch (ParserConfigurationException e) {
                throw new IOException(e.getMessage());
            } finally {
                stream.close();
            }
        }

        @Override
        protected Object getRootObject() {
            return artifacts;
        }

        protected class ArtifactsHandler extends RootHandler {

            private Set<IArtifactDescriptor> artifacts = new LinkedHashSet<>();

            public ArtifactsHandler() {
            }

            public Set<IArtifactDescriptor> getArtifacts() {
                return artifacts;
            }

            @Override
            public void startElement(String name, Attributes attributes) {
                if (name.equals(ARTIFACT_ELEMENT)) {
                    new ArtifactHandler(this, attributes, artifacts);
                } else {
                    invalidElement(name, attributes);
                }
            }

            @Override
            protected void handleRootAttributes(Attributes attributes) {
            }
        }

        public Set<IArtifactDescriptor> getArtifacts() {
            return artifacts;
        }

    }

    public Set<IArtifactDescriptor> readXML(InputStream is) throws IOException {
        Parser35M7 parser = new Parser35M7(Activator.getContext(), BundleConstants.BUNDLE_ID);

        parser.parse(is);

        return parser.getArtifacts();
    }

    public void writeXML(Set<? extends IArtifactDescriptor> descriptors, OutputStream os) throws IOException {
        new Writer35M7(os).write(descriptors);
    }

    public void writeXML(Set<? extends IArtifactDescriptor> descriptors, File file) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            writeXML(descriptors, os);
        }
    }
}
