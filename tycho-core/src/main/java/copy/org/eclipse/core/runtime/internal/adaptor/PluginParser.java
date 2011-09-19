/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package copy.org.eclipse.core.runtime.internal.adaptor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.internal.adaptor.EclipseAdaptorMsg;
import org.eclipse.core.runtime.internal.adaptor.IModel;
import org.eclipse.core.runtime.internal.adaptor.IPluginInfo;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Internal class.
 */
public class PluginParser extends DefaultHandler implements IModel {
	private static ServiceTracker xmlTracker = null;

	private PluginInfo manifestInfo = new PluginInfo();
//	private BundleContext context;
//	private FrameworkAdaptor adaptor;
	private Version target; // The targeted platform for the given manifest
	private static final Version TARGET21 = new Version(2, 1, 0);

	public class PluginInfo implements IPluginInfo {
		private String schemaVersion;
		private String pluginId;
		private String version;
		private String vendor;

		// an ordered list of library path names.
		private ArrayList libraryPaths;
		// TODO Should get rid of the libraries map and just have a
		// list of library export statements instead.  Library paths must
		// preserve order.
		private Map libraries; //represent the libraries and their export statement
		private ArrayList requires;
		private boolean requiresExpanded = false; //indicates if the requires have been processed.
		private boolean compatibilityFound = false; //set to true is the requirement list contain compatilibity 
		private String pluginClass;
		private String masterPluginId;
		private String masterVersion;
		private String masterMatch;
		private Set filters;
		private String pluginName;
		private boolean singleton;
		private boolean fragment;
		private final static String TARGET21_STRING = "2.1"; //$NON-NLS-1$
		private boolean hasExtensionExtensionPoints = false;

		public boolean isFragment() {
			return fragment;
		}

		public String toString() {
			return "plugin-id: " + pluginId + "  version: " + version + " libraries: " + libraries + " class:" + pluginClass + " master: " + masterPluginId + " master-version: " + masterVersion + " requires: " + requires + " singleton: " + singleton; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		}

		public Map getLibraries() {
			if (libraries == null)
				return new HashMap(0);
			return libraries;
		}

		public ArrayList getRequires() {
			if (!TARGET21.equals(target) && schemaVersion == null && !requiresExpanded) {
				requiresExpanded = true;
				if (requires == null) {
					requires = new ArrayList(1);
					requires.add(new Prerequisite(PluginConverterImpl.PI_RUNTIME, TARGET21_STRING, false, false, IModel.PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL));
					requires.add(new Prerequisite(PluginConverterImpl.PI_RUNTIME_COMPATIBILITY, null, false, false, null));
				} else {
					//Add elements on the requirement list of ui and help.
					for (int i = 0; i < requires.size(); i++) {
						Prerequisite analyzed = (Prerequisite) requires.get(i);
						if ("org.eclipse.ui".equals(analyzed.getName())) { //$NON-NLS-1$					
							requires.add(i + 1, new Prerequisite("org.eclipse.ui.workbench.texteditor", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
							requires.add(i + 1, new Prerequisite("org.eclipse.jface.text", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
							requires.add(i + 1, new Prerequisite("org.eclipse.ui.editors", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
							requires.add(i + 1, new Prerequisite("org.eclipse.ui.views", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
							requires.add(i + 1, new Prerequisite("org.eclipse.ui.ide", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
						} else if ("org.eclipse.help".equals(analyzed.getName())) { //$NON-NLS-1$ 
							requires.add(i + 1, new Prerequisite("org.eclipse.help.base", null, true, analyzed.isExported(), null)); //$NON-NLS-1$ 
						} else if (PluginConverterImpl.PI_RUNTIME.equals(analyzed.getName()) && !compatibilityFound) {
							requires.add(i + 1, new Prerequisite(PluginConverterImpl.PI_RUNTIME_COMPATIBILITY, null, false, analyzed.isExported(), null));
						}
					}
					if (!requires.contains(new Prerequisite(PluginConverterImpl.PI_RUNTIME_COMPATIBILITY, null, false, false, null))) {
						requires.add(new Prerequisite(PluginConverterImpl.PI_RUNTIME_COMPATIBILITY, null, false, false, null));
					}
					//Remove any prereq on runtime and add a prereq on runtime 2.1
					//This is used to recognize the version for which the given plugin was initially targeted.
					Prerequisite runtimePrereq = new Prerequisite(PluginConverterImpl.PI_RUNTIME, null, false, false, null);
					requires.remove(runtimePrereq);
					requires.add(new Prerequisite(PluginConverterImpl.PI_RUNTIME, TARGET21_STRING, false, false, IModel.PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL));
				}
			}
			if (requires == null)
				return requires = new ArrayList(0);

			return requires;
		}

		public String getMasterId() {
			return masterPluginId;
		}

		public String getMasterVersion() {
			return masterVersion;
		}

		public String getMasterMatch() {
			return masterMatch;
		}

		public String getPluginClass() {
			return pluginClass;
		}

		public String getUniqueId() {
			return pluginId;
		}

		public String getVersion() {
			return version;
		}

		public Set getPackageFilters() {
			return filters;
		}

		public String[] getLibrariesName() {
			if (libraryPaths == null)
				return new String[0];
			return (String[]) libraryPaths.toArray(new String[libraryPaths.size()]);
		}

		public String getPluginName() {
			return pluginName;
		}

		public String getProviderName() {
			return vendor;
		}

		public boolean isSingleton() {
			return singleton;
		}

		public boolean hasExtensionExtensionPoints() {
			return hasExtensionExtensionPoints;
		}
		
		public String getRoot() {
			return isFragment() ? FRAGMENT : PLUGIN;
		}

		/*
		 * Provides some basic form of validation. Since plugin/fragment is the only mandatory
		 * attribute, it is the only one we cara about here. 
		 */
		public String validateForm() {
			if (this.pluginId == null)
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_MISSING_ATTRIBUTE, new String[] {getRoot(), PLUGIN_ID, getRoot()});
			if (this.pluginName == null)
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_MISSING_ATTRIBUTE, new String[] {getRoot(), PLUGIN_NAME, getRoot()});
			if (this.version == null)
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_MISSING_ATTRIBUTE, new String[] {getRoot(), PLUGIN_VERSION, getRoot()});
			if (isFragment() && this.masterPluginId == null)
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_MISSING_ATTRIBUTE, new String[] {getRoot(), FRAGMENT_PLUGIN_ID, getRoot()});
			if (isFragment() && this.masterVersion == null)
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_MISSING_ATTRIBUTE, new String[] {getRoot(), FRAGMENT_PLUGIN_VERSION, getRoot()});
			return null;
		}
	}

	// Current State Information
	Stack stateStack = new Stack();

	// Current object stack (used to hold the current object we are populating in this plugin info
	Stack objectStack = new Stack();
	Locator locator = null;

	// Valid States
	private static final int IGNORED_ELEMENT_STATE = 0;
	private static final int INITIAL_STATE = 1;
	private static final int PLUGIN_STATE = 2;
	private static final int PLUGIN_RUNTIME_STATE = 3;
	private static final int PLUGIN_REQUIRES_STATE = 4;
	private static final int PLUGIN_EXTENSION_POINT_STATE = 5;
	private static final int PLUGIN_EXTENSION_STATE = 6;
	private static final int RUNTIME_LIBRARY_STATE = 7;
	private static final int LIBRARY_EXPORT_STATE = 8;
	private static final int PLUGIN_REQUIRES_IMPORT_STATE = 9;
	private static final int FRAGMENT_STATE = 11;

	public PluginParser(FrameworkAdaptor adaptor, BundleContext context, Version target) {
		super();
//		this.context = context;
//		this.adaptor = adaptor;
		this.target = target;
	}

	/**
	 * Receive a Locator object for document events.
	 * 
	 * <p>
	 * By default, do nothing. Application writers may override this method in
	 * a subclass if they wish to store the locator for use with other document
	 * events.
	 * </p>
	 * 
	 * @param locator A locator for all SAX document events.
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 * @see org.xml.sax.Locator
	 */
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	public void endDocument() {
	}

	public void endElement(String uri, String elementName, String qName) {
		switch (((Integer) stateStack.peek()).intValue()) {
			case IGNORED_ELEMENT_STATE :
				stateStack.pop();
				break;
			case INITIAL_STATE :
				// shouldn't get here
				// internalError(Policy.bind("parse.internalStack", elementName)); //$NON-NLS-1$
				break;
			case PLUGIN_STATE :
			case FRAGMENT_STATE :
				break;
			case PLUGIN_RUNTIME_STATE :
				if (elementName.equals(RUNTIME)) {
					stateStack.pop();
				}
				break;
			case PLUGIN_REQUIRES_STATE :
				if (elementName.equals(PLUGIN_REQUIRES)) {
					stateStack.pop();
					objectStack.pop();
				}
				break;
			case PLUGIN_EXTENSION_POINT_STATE :
				if (elementName.equals(EXTENSION_POINT)) {
					stateStack.pop();
				}
				break;
			case PLUGIN_EXTENSION_STATE :
				if (elementName.equals(EXTENSION)) {
					stateStack.pop();
				}
				break;
			case RUNTIME_LIBRARY_STATE :
				if (elementName.equals(LIBRARY)) {
					String curLibrary = (String) objectStack.pop();
					if (!curLibrary.trim().equals("")) { //$NON-NLS-1$
						Vector exportsVector = (Vector) objectStack.pop();
						if (manifestInfo.libraries == null) {
							manifestInfo.libraries = new HashMap(3);
							manifestInfo.libraryPaths = new ArrayList(3);
						}
						manifestInfo.libraries.put(curLibrary, exportsVector);
						manifestInfo.libraryPaths.add(curLibrary.replace('\\', '/'));
					}
					stateStack.pop();
				}
				break;
			case LIBRARY_EXPORT_STATE :
				if (elementName.equals(LIBRARY_EXPORT)) {
					stateStack.pop();
				}
				break;
			case PLUGIN_REQUIRES_IMPORT_STATE :
				if (elementName.equals(PLUGIN_REQUIRES_IMPORT)) {
					stateStack.pop();
				}
				break;
		}
	}

	public void error(SAXParseException ex) {
		logStatus(ex);
	}

	public void fatalError(SAXParseException ex) throws SAXException {
		logStatus(ex);
		throw ex;
	}

	public void handleExtensionPointState(String elementName, Attributes attributes) {
		// nothing to do for extension-points' children
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		manifestInfo.hasExtensionExtensionPoints = true;
	}

	public void handleExtensionState(String elementName, Attributes attributes) {
		// nothing to do for extensions' children
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		manifestInfo.hasExtensionExtensionPoints = true;
	}

	public void handleInitialState(String elementName, Attributes attributes) {
		if (elementName.equals(PLUGIN)) {
			stateStack.push(new Integer(PLUGIN_STATE));
			parsePluginAttributes(attributes);
		} else if (elementName.equals(FRAGMENT)) {
			manifestInfo.fragment = true;
			stateStack.push(new Integer(FRAGMENT_STATE));
			parseFragmentAttributes(attributes);
		} else {
			stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
			internalError(elementName);
		}
	}

	public void handleLibraryExportState(String elementName, Attributes attributes) {
		// All elements ignored.
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}

	public void handleLibraryState(String elementName, Attributes attributes) {
		if (elementName.equals(LIBRARY_EXPORT)) {
			// Change State
			stateStack.push(new Integer(LIBRARY_EXPORT_STATE));
			// The top element on the stack much be a library element
			String currentLib = (String) objectStack.peek();
			if (attributes == null)
				return;
			String maskValue = attributes.getValue("", LIBRARY_EXPORT_MASK); //$NON-NLS-1$
			// pop off the library - already in currentLib
			objectStack.pop();
			Vector exportMask = (Vector) objectStack.peek();
			// push library back on
			objectStack.push(currentLib);
			//Split the export upfront
			if (maskValue != null) {
				StringTokenizer tok = new StringTokenizer(maskValue, ","); //$NON-NLS-1$
				while (tok.hasMoreTokens()) {
					String value = tok.nextToken();
					if (!exportMask.contains(maskValue))
						exportMask.addElement(value.trim());
				}
			}
			return;
		}
		if (elementName.equals(LIBRARY_PACKAGES)) {
			stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
			return;
		}
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		internalError(elementName);
		return;
	}

	public void handlePluginState(String elementName, Attributes attributes) {
		if (elementName.equals(RUNTIME)) {
			// We should only have one Runtime element in a plugin or fragment
			Object whatIsIt = objectStack.peek();
			if ((whatIsIt instanceof PluginInfo) && ((PluginInfo) objectStack.peek()).libraries != null) {
				// This is at least the 2nd Runtime element we have hit. Ignore it.
				stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
				return;
			}
			stateStack.push(new Integer(PLUGIN_RUNTIME_STATE));
			// Push a new vector to hold all the library entries objectStack.push(new Vector());
			return;
		}
		if (elementName.equals(PLUGIN_REQUIRES)) {
			stateStack.push(new Integer(PLUGIN_REQUIRES_STATE));
			// Push a new vector to hold all the prerequisites
			objectStack.push(new Vector());
			parseRequiresAttributes(attributes);
			return;
		}
		if (elementName.equals(EXTENSION_POINT)) {
			// mark the plugin as singleton and ignore all elements under extension (if there are any)
			manifestInfo.singleton = true;
			stateStack.push(new Integer(PLUGIN_EXTENSION_POINT_STATE));
			return;
		}
		if (elementName.equals(EXTENSION)) {
			// mark the plugin as singleton and ignore all elements under extension (if there are any)
			manifestInfo.singleton = true;
			stateStack.push(new Integer(PLUGIN_EXTENSION_STATE));
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		internalError(elementName);
	}

	public void handleRequiresImportState(String elementName, Attributes attributes) {
		// All elements ignored.
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
	}

	public void handleRequiresState(String elementName, Attributes attributes) {
		if (elementName.equals(PLUGIN_REQUIRES_IMPORT)) {
			parsePluginRequiresImport(attributes);
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		internalError(elementName);
	}

	public void handleRuntimeState(String elementName, Attributes attributes) {
		if (elementName.equals(LIBRARY)) {
			// Change State
			stateStack.push(new Integer(RUNTIME_LIBRARY_STATE));
			// Process library attributes
			parseLibraryAttributes(attributes);
			return;
		}
		// If we get to this point, the element name is one we don't currently accept.
		// Set the state to indicate that this element will be ignored
		stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		internalError(elementName);
	}

	private void logStatus(SAXParseException ex) {
		String name = ex.getSystemId();
		if (name == null)
			name = ""; //$NON-NLS-1$ 
		else
			name = name.substring(1 + name.lastIndexOf("/")); //$NON-NLS-1$ 
		String msg;
		if (name.equals("")) //$NON-NLS-1$ 
			msg = NLS.bind(EclipseAdaptorMsg.parse_error, ex.getMessage());
		else
			msg = NLS.bind(EclipseAdaptorMsg.parse_errorNameLineColumn, new String[] {name, Integer.toString(ex.getLineNumber()), Integer.toString(ex.getColumnNumber()), ex.getMessage()});

		FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, msg, 0, ex, null);
//		adaptor.getFrameworkLog().log(entry);
	}

	synchronized public PluginInfo parsePlugin(InputStream in) throws Exception {
		SAXParserFactory factory = acquireXMLParsing(null);
		if (factory == null) {
			FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, EclipseAdaptorMsg.ECLIPSE_CONVERTER_NO_SAX_FACTORY, 0, null, null);
//			adaptor.getFrameworkLog().log(entry);
			return null;
		}

		factory.setNamespaceAware(true);
		factory.setNamespaceAware(true);
		try {
			factory.setFeature("http://xml.org/sax/features/string-interning", true); //$NON-NLS-1$
		} catch (SAXException se) {
			// ignore; we can still operate without string-interning
		}
		factory.setValidating(false);
		factory.newSAXParser().parse(in, this);
		return manifestInfo;
	}

	public static SAXParserFactory acquireXMLParsing(BundleContext context) {
//		if (xmlTracker == null) {
//			xmlTracker = new ServiceTracker(context, "javax.xml.parsers.SAXParserFactory", null); //$NON-NLS-1$
//			xmlTracker.open();
//		}
//		SAXParserFactory result = (SAXParserFactory) xmlTracker.getService();
//		if (result != null)
//			return result;
		// backup to using jaxp to create a new instance
		return SAXParserFactory.newInstance();
	}

	public static void releaseXMLParsing() {
		if (xmlTracker != null)
			xmlTracker.close();
	}

	public void parseFragmentAttributes(Attributes attributes) {
		// process attributes
		objectStack.push(manifestInfo);
		int len = attributes.getLength();
		for (int i = 0; i < len; i++) {
			String attrName = attributes.getLocalName(i);
			String attrValue = attributes.getValue(i).trim();
			if (attrName.equals(FRAGMENT_ID))
				manifestInfo.pluginId = attrValue;
			else if (attrName.equals(FRAGMENT_NAME))
				manifestInfo.pluginName = attrValue;
			else if (attrName.equals(FRAGMENT_VERSION))
				manifestInfo.version = attrValue;
			else if (attrName.equals(FRAGMENT_PROVIDER))
				manifestInfo.vendor = attrValue;
			else if (attrName.equals(FRAGMENT_PLUGIN_ID))
				manifestInfo.masterPluginId = attrValue;
			else if (attrName.equals(FRAGMENT_PLUGIN_VERSION))
				manifestInfo.masterVersion = attrValue;
			else if (attrName.equals(FRAGMENT_PLUGIN_MATCH))
				manifestInfo.masterMatch = attrValue;
		}
	}

	public void parseLibraryAttributes(Attributes attributes) {
		// Push a vector to hold the export mask
		objectStack.push(new Vector());
		String current = attributes.getValue("", LIBRARY_NAME); //$NON-NLS-1$ 
		objectStack.push(current);
	}

	public void parsePluginAttributes(Attributes attributes) {
		// process attributes
		objectStack.push(manifestInfo);
		int len = attributes.getLength();
		for (int i = 0; i < len; i++) {
			String attrName = attributes.getLocalName(i);
			String attrValue = attributes.getValue(i).trim();
			if (attrName.equals(PLUGIN_ID))
				manifestInfo.pluginId = attrValue;
			else if (attrName.equals(PLUGIN_NAME))
				manifestInfo.pluginName = attrValue;
			else if (attrName.equals(PLUGIN_VERSION))
				manifestInfo.version = attrValue;
			else if (attrName.equals(PLUGIN_VENDOR) || (attrName.equals(PLUGIN_PROVIDER)))
				manifestInfo.vendor = attrValue;
			else if (attrName.equals(PLUGIN_CLASS))
				manifestInfo.pluginClass = attrValue;
		}
	}

	public class Prerequisite {
		String name;
		String version;
		boolean optional;
		boolean export;
		String match;

		public boolean isExported() {
			return export;
		}

		public String getMatch() {
			return match;
		}

		public String getName() {
			return name;
		}

		public boolean isOptional() {
			return optional;
		}

		public String getVersion() {
			return version;
		}

		public Prerequisite(String preqName, String prereqVersion, boolean isOtional, boolean isExported, String prereqMatch) {
			name = preqName;
			version = prereqVersion;
			optional = isOtional;
			export = isExported;
			match = prereqMatch;
		}

		public String toString() {
			return name;
		}

		public boolean equals(Object prereq) {
			if (!(prereq instanceof Prerequisite))
				return false;
			return name.equals(((Prerequisite) prereq).name);
		}
	}

	public void parsePluginRequiresImport(Attributes attributes) {
		if (manifestInfo.requires == null) {
			manifestInfo.requires = new ArrayList();
			// to avoid cycles
			//			if (!manifestInfo.pluginId.equals(PluginConverterImpl.PI_RUNTIME))  //$NON-NLS-1$
			//				manifestInfo.requires.add(new Prerequisite(PluginConverterImpl.PI_RUNTIME, null, false, false, null)); //$NON-NLS-1$
		}
		// process attributes
		String plugin = attributes.getValue("", PLUGIN_REQUIRES_PLUGIN); //$NON-NLS-1$ 
		if (plugin == null)
			return;
		if (plugin.equals(PluginConverterImpl.PI_BOOT)) 
			return;
		if (plugin.equals(PluginConverterImpl.PI_RUNTIME_COMPATIBILITY))
			manifestInfo.compatibilityFound = true;
		String version = attributes.getValue("", PLUGIN_REQUIRES_PLUGIN_VERSION); //$NON-NLS-1$ 
		String optional = attributes.getValue("", PLUGIN_REQUIRES_OPTIONAL); //$NON-NLS-1$ 
		String export = attributes.getValue("", PLUGIN_REQUIRES_EXPORT); //$NON-NLS-1$ 
		String match = attributes.getValue("", PLUGIN_REQUIRES_MATCH); //$NON-NLS-1$
		manifestInfo.requires.add(new Prerequisite(plugin, version, "true".equalsIgnoreCase(optional) ? true : false, "true".equalsIgnoreCase(export) ? true : false, match)); //$NON-NLS-1$  //$NON-NLS-2$
	}

	public void parseRequiresAttributes(Attributes attributes) {
		//Nothing to do.
	}

	static String replace(String s, String from, String to) {
		String str = s;
		int fromLen = from.length();
		int toLen = to.length();
		int ix = str.indexOf(from);
		while (ix != -1) {
			str = str.substring(0, ix) + to + str.substring(ix + fromLen);
			ix = str.indexOf(from, ix + toLen);
		}
		return str;
	}

	public void startDocument() {
		stateStack.push(new Integer(INITIAL_STATE));
	}

	public void startElement(String uri, String elementName, String qName, Attributes attributes) {
		switch (((Integer) stateStack.peek()).intValue()) {
			case INITIAL_STATE :
				handleInitialState(elementName, attributes);
				break;
			case FRAGMENT_STATE :
			case PLUGIN_STATE :
				handlePluginState(elementName, attributes);
				break;
			case PLUGIN_RUNTIME_STATE :
				handleRuntimeState(elementName, attributes);
				break;
			case PLUGIN_REQUIRES_STATE :
				handleRequiresState(elementName, attributes);
				break;
			case PLUGIN_EXTENSION_POINT_STATE :
				handleExtensionPointState(elementName, attributes);
				break;
			case PLUGIN_EXTENSION_STATE :
				handleExtensionState(elementName, attributes);
				break;
			case RUNTIME_LIBRARY_STATE :
				handleLibraryState(elementName, attributes);
				break;
			case LIBRARY_EXPORT_STATE :
				handleLibraryExportState(elementName, attributes);
				break;
			case PLUGIN_REQUIRES_IMPORT_STATE :
				handleRequiresImportState(elementName, attributes);
				break;
			default :
				stateStack.push(new Integer(IGNORED_ELEMENT_STATE));
		}
	}

	public void warning(SAXParseException ex) {
		logStatus(ex);
	}

	private void internalError(String elementName) {
		FrameworkLogEntry error;
		String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_PARSE_UNKNOWNTOP_ELEMENT, elementName); 
		error = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, (manifestInfo.pluginId == null ? message : "Plug-in : " + manifestInfo.pluginId + ", " + message), 0, null, null); //$NON-NLS-1$ //$NON-NLS-2$
//		adaptor.getFrameworkLog().log(error);
	}

	public void processingInstruction(String target, String data) throws SAXException {
		// Since 3.0, a processing instruction of the form <?eclipse version="3.0"?> at
		// the start of the manifest file is used to indicate the plug-in manifest
		// schema version in effect. Pre-3.0 (i.e., 2.1) plug-in manifest files do not
		// have one of these, and this is how we can distinguish the manifest of a
		// pre-3.0 plug-in from a post-3.0 one (for compatibility tranformations).
		if (target.equalsIgnoreCase("eclipse")) { //$NON-NLS-1$ 
			// just the presence of this processing instruction indicates that this
			// plug-in is at least 3.0
			manifestInfo.schemaVersion = "3.0"; //$NON-NLS-1$ 
			StringTokenizer tokenizer = new StringTokenizer(data, "=\""); //$NON-NLS-1$ 
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equalsIgnoreCase("version")) { //$NON-NLS-1$ 
					if (!tokenizer.hasMoreTokens()) {
						break;
					}
					manifestInfo.schemaVersion = tokenizer.nextToken();
					break;
				}
			}
		}
	}
}
