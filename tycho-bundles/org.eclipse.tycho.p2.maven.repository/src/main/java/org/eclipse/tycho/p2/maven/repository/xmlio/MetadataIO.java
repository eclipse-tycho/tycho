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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.repository.Messages;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataParser;
import org.eclipse.equinox.internal.p2.metadata.repository.io.MetadataWriter;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.tycho.p2.maven.repository.Activator;
import org.eclipse.tycho.repository.util.internal.BundleConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class MetadataIO {

    private interface XMLConstants extends org.eclipse.equinox.internal.p2.metadata.repository.io.XMLConstants {

        // Constants defining the structure of the XML for a MetadataRepository

        // A format version number for metadata repository XML.
        public static final Version COMPATIBLE_VERSION = Version.createOSGi(1, 0, 0);
        public static final Version CURRENT_VERSION = Version.createOSGi(1, 1, 0);
        public static final VersionRange XML_TOLERANCE = new VersionRange(COMPATIBLE_VERSION, true,
                Version.createOSGi(2, 0, 0), false);

        // Constants for processing Instructions
        public static final String PI_REPOSITORY_TARGET = "metadataRepository"; //$NON-NLS-1$

        // Constants for metadata repository elements
        public static final String REPOSITORY_ELEMENT = "repository"; //$NON-NLS-1$

    }

    private static class Writer extends MetadataWriter implements XMLConstants {

        public Writer(OutputStream output) throws UnsupportedEncodingException {
            super(output, null);
        }

        public void write(Set<IInstallableUnit> units, Set<IRepositoryReference> references) {
            start(REPOSITORY_ELEMENT);

            writeInstallableUnits(units.iterator(), units.size());

            writeRepositoryReferences(references.iterator(), references.size());

            end(REPOSITORY_ELEMENT);

            flush();
        }

        private void writeRepositoryReferences(Iterator<IRepositoryReference> references, int size) {
            if (size == 0)
                return;
            start(REPOSITORY_REFERENCES_ELEMENT);
            attribute(COLLECTION_SIZE_ATTRIBUTE, size);
            while (references.hasNext())
                writeRepositoryReference(references.next());
            end(REPOSITORY_REFERENCES_ELEMENT);
        }

        private void writeRepositoryReference(IRepositoryReference reference) {
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

    private static class Parser extends MetadataParser implements XMLConstants {
        public enum PARSER_MODE {
            REPO, IU
        }

        private PARSER_MODE mode;

        private List<InstallableUnitDescription> units;
        private Set<IRepositoryReference> references;
        private OrderedProperties properties;

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
                RepositoryRootHandler handler = new RepositoryRootHandler(mode);
                xmlReader.setContentHandler(handler);

                xmlReader.parse(new InputSource(stream));
                if (isValidXML()) {
                    units = handler.getUnits();
                    references = handler.getRepoRefs();
                    properties = handler.getProperties();
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

        private final class RepositoryRootHandler extends RootHandler {
            private RepositoryHandler repositoryHandler;

            public RepositoryRootHandler(PARSER_MODE mode) {
                super();
            }

            public Set<IRepositoryReference> getRepoRefs() {
                if (repositoryHandler == null) {
                    return new HashSet<>();
                }
                return repositoryHandler.getRepoRefs();
            }

            public List<InstallableUnitDescription> getUnits() {
                if (repositoryHandler == null) {
                    return new ArrayList<>();
                }
                return repositoryHandler.getUnits();
            }

            public OrderedProperties getProperties() {
                if (repositoryHandler == null) {
                    return null;
                }
                return repositoryHandler.getProperties();
            }

            public void startElement(String name, Attributes attributes) throws SAXException {
                checkCancel();
                if (mode.equals(PARSER_MODE.REPO)) {
                    if (REPOSITORY_ELEMENT.equals(name)) {
                        if (repositoryHandler == null) {
                            repositoryHandler = new RepositoryHandler(this, attributes);
                        } else {
                            duplicateElement(this, name, attributes);
                        }
                    } else {
                        invalidElement(name, attributes);
                    }
                } else if (mode.equals(PARSER_MODE.IU)) {
                    new InstallableUnitsHandler(this, attributes);
                }
            }

            @Override
            protected void handleRootAttributes(Attributes attributes) {
                // TODO Auto-generated method stub

            }
        }

        private abstract class AbstractSizedMetadataHandler extends AbstractMetadataHandler {

            public AbstractSizedMetadataHandler(ContentHandler parentHandler, String elementHandled) {
                super(parentHandler, elementHandled);
            }

            protected int getOptionalSize(Attributes attributes, int dflt) {
                String sizeStr = parseOptionalAttribute(attributes, COLLECTION_SIZE_ATTRIBUTE);
                return sizeStr != null ? Integer.parseInt(sizeStr) : dflt;
            }
        }

        protected class RepositoryHandler extends AbstractMetadataHandler {
            private InstallableUnitsHandler unitsHandler;
            private PropertiesHandler propertiesHandler;
            private RepositoryReferencesHandler repositoryReferencesHandler;

            public RepositoryHandler(RepositoryRootHandler parentHandler, Attributes attributes) {
                super(parentHandler, REPOSITORY_ELEMENT);
            }

            public Set<IRepositoryReference> getRepoRefs() {
                if (repositoryReferencesHandler == null) {
                    return new HashSet<>();
                }
                return repositoryReferencesHandler.getRepoRefs();
            }

            public List<InstallableUnitDescription> getUnits() {
                if (unitsHandler == null) {
                    return new ArrayList<>();
                }
                return unitsHandler.getUnits();
            }

            public OrderedProperties getProperties() {
                if (propertiesHandler == null) {
                    return null;
                }
                return propertiesHandler.getProperties();
            }

            public void startElement(String name, Attributes attributes) {
                if (PROPERTIES_ELEMENT.equals(name)) {
                    if (propertiesHandler == null) {
                        propertiesHandler = new PropertiesHandler(this, attributes);
                    } else {
                        duplicateElement(this, name, attributes);
                    }
                } else if (INSTALLABLE_UNITS_ELEMENT.equals(name)) {
                    if (unitsHandler == null) {
                        unitsHandler = new InstallableUnitsHandler(this, attributes);
                    } else {
                        duplicateElement(this, name, attributes);
                    }
                } else if (REPOSITORY_REFERENCES_ELEMENT.equals(name)) {
                    if (repositoryReferencesHandler == null) {
                        repositoryReferencesHandler = new RepositoryReferencesHandler(this, attributes);
                    } else {
                        duplicateElement(this, name, attributes);
                    }
                } else {
                    invalidElement(name, attributes);
                }
            }
        }

        protected class InstallableUnitsHandler extends AbstractSizedMetadataHandler {
            private ArrayList<InstallableUnitDescription> units;

            public InstallableUnitsHandler(AbstractHandler parentHandler, Attributes attributes) {
                super(parentHandler, INSTALLABLE_UNITS_ELEMENT);
                units = new ArrayList<InstallableUnitDescription>(getOptionalSize(attributes, 4));
            }

            public List<InstallableUnitDescription> getUnits() {
                return units;
            }

            public void startElement(String name, Attributes attributes) {
                if (name.equals(INSTALLABLE_UNIT_ELEMENT)) {
                    new InstallableUnitHandler(this, attributes, units);
                } else {
                    invalidElement(name, attributes);
                }
            }
        }

        private final class RepositoryReferencesHandler extends AbstractSizedMetadataHandler {

            private Set<IRepositoryReference> repoRefs = new LinkedHashSet<>();

            public RepositoryReferencesHandler(AbstractHandler parentHandler, Attributes attributes) {
                super(parentHandler, REPOSITORY_REFERENCES_ELEMENT);
                references = new HashSet<IRepositoryReference>(getOptionalSize(attributes, 4));
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

        public OrderedProperties getProperties() {
            return properties;
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

        result.setProperties(parser.getProperties());

        return result;
    }

    public class ReadXmlResult {
        private LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<>();
        private LinkedHashSet<IRepositoryReference> references = new LinkedHashSet<>();
        private OrderedProperties properties = null;

        public Set<IInstallableUnit> getUnits() {
            return Collections.unmodifiableSet(units);
        }

        public void setProperties(OrderedProperties properties) {
            this.properties = properties;
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

    public void writeXML(Set<IInstallableUnit> units, Set<IRepositoryReference> references, OutputStream os)
            throws IOException {
        Writer writer = new Writer(os);
        writer.write(units, references);
    }

    public void writeXML(Set<IInstallableUnit> units, Set<IRepositoryReference> references, File file)
            throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            writeXML(units, references, os);
        } finally {
            os.close();
        }
    }

}
