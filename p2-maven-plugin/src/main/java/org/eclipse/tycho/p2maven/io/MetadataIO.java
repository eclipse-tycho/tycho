/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Rapicorp, Inc. - Support for IU packaging type
 *******************************************************************************/
package org.eclipse.tycho.p2maven.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.metadata.repository.Messages;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.tycho.p2maven.P2Plugin;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Named
@Singleton
public class MetadataIO {
    private static class Writer extends MetadataWriter {

        public Writer(OutputStream output) {
            super(output, null);
        }

		public void write(Collection<? extends IInstallableUnit> units) {
            start(INSTALLABLE_UNITS_ELEMENT);

            attribute(COLLECTION_SIZE_ATTRIBUTE, units.size());
            for (IInstallableUnit unit : units) {
                writeInstallableUnit(unit);
            }

            end(INSTALLABLE_UNITS_ELEMENT);
            flush();
        }

    }

    private static class Parser extends MetadataParser {
        public enum PARSER_MODE {
            REPO, IU
        }

        private PARSER_MODE mode;

        private List<InstallableUnitDescription> units;

        public Parser(PARSER_MODE mode) {
			super(SAXParserFactory.newInstance(), P2Plugin.BUNDLE_ID);
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
                if (mode.equals(PARSER_MODE.REPO))
                    xmlReader.setContentHandler(new RepositoryDocHandler(INSTALLABLE_UNITS_ELEMENT, handler));
                else
                    xmlReader.setContentHandler(handler);

                xmlReader.parse(new InputSource(stream));
                if (isValidXML()) {
                    units = handler.getUnits();
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

        public List<InstallableUnitDescription> getUnits() {
            return units;
        }
    }

    public InstallableUnitDescription readOneIU(InputStream is) throws IOException {
        Parser parser = new Parser(Parser.PARSER_MODE.IU);

        parser.parse(is, new NullProgressMonitor());
        return parser.getUnits().get(0);
    }

    public Set<IInstallableUnit> readXML(InputStream is) throws IOException {
        Parser parser = new Parser(Parser.PARSER_MODE.REPO);

        parser.parse(is, new NullProgressMonitor());

        Set<IInstallableUnit> units = new LinkedHashSet<>();

        for (InstallableUnitDescription desc : parser.getUnits()) {
            units.add(MetadataFactory.createInstallableUnit(desc));
        }

        return units;
    }

	public void writeXML(Collection<? extends IInstallableUnit> units, OutputStream os) throws IOException {
        new Writer(os).write(units);
    }

	public void writeXML(Collection<? extends IInstallableUnit> units, File file) throws IOException {
		file.getParentFile().mkdirs();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            writeXML(units, os);
        }
    }

	public Set<IInstallableUnit> readXML(File artifact) throws IOException {
		try (FileInputStream stream = new FileInputStream(artifact)) {
			return readXML(stream);
		}

	}
}
