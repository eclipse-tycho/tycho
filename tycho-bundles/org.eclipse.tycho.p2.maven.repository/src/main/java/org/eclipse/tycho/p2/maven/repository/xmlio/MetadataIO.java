/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - Support for IU packaging type
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository.xmlio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.repository.Messages;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.util.internal.BundleConstants;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class MetadataIO {
    private static class Writer extends MetadataWriter {

        public Writer(OutputStream output) throws UnsupportedEncodingException {
            super(output, null);
        }

        public void write(Set<IInstallableUnit> units) {
            start(INSTALLABLE_UNITS_ELEMENT);

            attribute(COLLECTION_SIZE_ATTRIBUTE, units.size());
            for (IInstallableUnit unit : units) {
                writeInstallableUnit(unit);
            }

            end(INSTALLABLE_UNITS_ELEMENT);
            flush();
        }

        public void writeRepositoryReferences(Set<IRepositoryReference> references) {
            start(REPOSITORY_REFERENCES_ELEMENT);

            attribute(COLLECTION_SIZE_ATTRIBUTE, references.size());
            for (IRepositoryReference reference : references) {
                writeInstallableUnit(reference);
            }

            end(REPOSITORY_REFERENCES_ELEMENT);
            flush();
        }

        private void writeInstallableUnit(IRepositoryReference reference) {
            start(REPOSITORY_REFERENCE_ELEMENT);
            attribute(URI_ATTRIBUTE, reference.getLocation().toString());

            try {
                // we write the URL attribute for backwards compatibility with 3.4.x
                // this attribute should be removed if we make a breaking format change.
                attribute(URL_ATTRIBUTE, URIUtil.toURL(reference.getLocation()).toExternalForm());
            } catch (MalformedURLException e) {
                attribute(URL_ATTRIBUTE, reference.getLocation().toString());
            }

            attribute(TYPE_ATTRIBUTE, Integer.toString(reference.getType()));
            attribute(OPTIONS_ATTRIBUTE, Integer.toString(reference.getOptions()));
            end(REPOSITORY_REFERENCE_ELEMENT);
        }

    }

    private static class Parser extends MetadataParser {
        public enum PARSER_MODE {
            REPO, IU
        }

        private PARSER_MODE mode;

        private List<InstallableUnitDescription> units;
        private Set<IRepositoryReference> references;

        public Parser(PARSER_MODE mode) {
            super(Activator.getContext(), BundleConstants.BUNDLE_ID);
            this.mode = mode;
        }

        @Override
        protected String getErrorMessage() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected Object getRootObject() {
            // TODO Auto-generated method stub
            return null;
        }

        public synchronized void parse(InputStream stream, IProgressMonitor monitor) throws IOException {
            this.status = null;
            setProgressMonitor(monitor);
            monitor.beginTask(Messages.repo_loading, IProgressMonitor.UNKNOWN);
            try {
                // TODO: currently not caching the parser since we make no assumptions
                // or restrictions on concurrent parsing
                getParser();
                InstallableUnitsHandler handler = new InstallableUnitsHandler();
                RepositoryReferencesHandler repoRefHandler = new RepositoryReferencesHandler();
                if (mode.equals(PARSER_MODE.REPO)) {
                    xmlReader.setContentHandler(new RepositoryDocHandler(INSTALLABLE_UNITS_ELEMENT, handler));
                    xmlReader
                            .setContentHandler(new RepositoryDocHandler(REPOSITORY_REFERENCES_ELEMENT, repoRefHandler));
                } else
                    xmlReader.setContentHandler(handler);

                xmlReader.parse(new InputSource(stream));
                if (isValidXML()) {
                    units = handler.getUnits();
                    references = repoRefHandler.getRepoRefs();
                }
            } catch (SAXException e) {
                if (!(e.getException() instanceof OperationCanceledException))
                    throw new IOException(e.getMessage());
            } catch (ParserConfigurationException e) {
                throw new IOException(e.getMessage());
            } finally {
                monitor.done();
                stream.close();
            }
        }

        private final class RepositoryDocHandler extends DocHandler {

            public RepositoryDocHandler(String rootName, RootHandler rootHandler) {
                super(rootName, rootHandler);
            }

            @Override
            public void processingInstruction(String target, String data) throws SAXException {
                // if (PI_REPOSITORY_TARGET.equals(target)) {
                // Version repositoryVersion = extractPIVersion(target, data);
                // if (!MetadataRepositoryIO.XMLConstants.XML_TOLERANCE.isIncluded(repositoryVersion)) {
                // throw new SAXException(NLS.bind(Messages.io_IncompatibleVersion, repositoryVersion,
                // MetadataRepositoryIO.XMLConstants.XML_TOLERANCE));
                // }
                // }
            }
        }

        private final class InstallableUnitsHandler extends RootHandler {

            private List<InstallableUnitDescription> units = new ArrayList<>();

            @Override
            protected void handleRootAttributes(Attributes attributes) {
                // TODO Auto-generated method stub

            }

            public List<InstallableUnitDescription> getUnits() {
                return units;
            }

            @Override
            public void startElement(String name, Attributes attributes) throws SAXException {
                if (name.equals(INSTALLABLE_UNIT_ELEMENT)) {
                    new InstallableUnitHandler(this, attributes, units);
                } else {
                    invalidElement(name, attributes);
                }
            }
        }

        private final class RepositoryReferencesHandler extends RootHandler {

            private Set<IRepositoryReference> repoRefs = new LinkedHashSet<>();

            @Override
            protected void handleRootAttributes(Attributes attributes) {
                // not used
            }

            public Set<IRepositoryReference> getRepoRefs() {
                return repoRefs;
            }

            @Override
            public void startElement(String name, Attributes attributes) throws SAXException {
                if (name.equals(REPOSITORY_REFERENCE_ELEMENT)) {
                    new RepositoryReferenceHandler(this, attributes, repoRefs);
                } else {
                    invalidElement(name, attributes);
                }
            }
        }

        public List<InstallableUnitDescription> getUnits() {
            return units;
        }

        public Set<IRepositoryReference> getRepoRefs() {
            return references;
        }
    }

    public InstallableUnitDescription readOneIU(InputStream is) throws IOException {
        Parser parser = new Parser(Parser.PARSER_MODE.IU);

        parser.parse(is, new NullProgressMonitor());
        return parser.getUnits().get(0);
    }

    public ReadXmlResult readXML(InputStream is) throws IOException {
        Parser parser = new Parser(Parser.PARSER_MODE.REPO);

        parser.parse(is, new NullProgressMonitor());

        ReadXmlResult result = new ReadXmlResult();

        for (InstallableUnitDescription desc : parser.getUnits()) {
            result.add(MetadataFactory.createInstallableUnit(desc));
        }

        for (IRepositoryReference desc : parser.getRepoRefs()) {
            result.add(desc);
        }

        return result;
    }

    public class ReadXmlResult {
        private LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<>();
        private LinkedHashSet<IRepositoryReference> references = new LinkedHashSet<>();

        public Set<IInstallableUnit> getUnits() {
            return Collections.unmodifiableSet(units);
        }

        public Set<IRepositoryReference> getRepoRefs() {
            return Collections.unmodifiableSet(references);
        }

        public void add(IInstallableUnit unit) {
            units.add(unit);
        }

        public void add(IRepositoryReference ref) {
            references.add(ref);
        }
    }

    public void writeInstallableUnits(Set<IInstallableUnit> units, OutputStream os) throws IOException {
        new Writer(os).write(units);
    }

    private void writeRepositoryReferences(Set<IRepositoryReference> references, OutputStream os) throws IOException {
        new Writer(os).writeRepositoryReferences(references);
    }

    public void writeXML(Set<IInstallableUnit> units, Set<IRepositoryReference> references, File file)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            writeInstallableUnits(units, os);
            writeRepositoryReferences(references, os);
        } finally {
            os.close();
        }
    }

}
