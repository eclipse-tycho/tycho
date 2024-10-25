/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chrsitoph Läubrich - adapt for using with Tycho
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle.runner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.schema.PathSchemaProvider;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaProvider;
import org.osgi.framework.Constants;

/**
 * This class is based on org.eclipse.pde.internal.core.ant.ConvertSchemaToHTML
 */
public class ConvertSchemaToHtmlRunner implements Callable<ConvertSchemaToHtmlResult>, Serializable {
	private List<File> manifests;
	private File destination;
	private URL cssURL;
	private List<String> additionalSearchPaths;
	private File baseDir;

	public ConvertSchemaToHtmlRunner(List<File> manifests, File destination, URL cssURL,
			List<String> additionalSearchPaths, File baseDir) {
		this.manifests = manifests;
		this.destination = destination;
		this.cssURL = cssURL;
		this.additionalSearchPaths = additionalSearchPaths;
		this.baseDir = baseDir;
	}

	@Override
	public ConvertSchemaToHtmlResult call() throws Exception {
		SchemaTransformer fTransformer = new SchemaTransformer();
		ConvertSchemaToHtmlResult result = new ConvertSchemaToHtmlResult();
		for (File manifest : manifests) {

			IPluginModelBase model = readManifestFile(manifest);
			if (model == null) {
				return result;
			}

			String pluginID = model.getPluginBase().getId();
			if (pluginID == null) {
				pluginID = getPluginID(manifest);
			}

			IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
			for (IPluginExtensionPoint extPoint : extPoints) {
				String schemaLocation = extPoint.getSchema();

				if (schemaLocation == null || schemaLocation.equals("")) { //$NON-NLS-1$
					continue;
				}
				Schema schema = null;
				try {
					File schemaFile = new File(model.getInstallLocation(), schemaLocation);
					XMLDefaultHandler handler = new XMLDefaultHandler();
					org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE()
							.parse(schemaFile, handler);
					@SuppressWarnings("deprecation")
					URL url = schemaFile.toURL();
					PathSchemaProvider pathSchemaProvider = new PathSchemaProvider(
							additionalSearchPaths.stream().map(pathString -> {
								IPath path = IPath.fromOSString(pathString);
								if (!path.isAbsolute()) {
									return IPath.fromOSString(baseDir.getPath()).append(path);
								}
								return path;
							}).toList());
					SchemaDescriptor desc = new SchemaDescriptor(extPoint.getFullId(), url, new SchemaProvider() {

						@Override
						public ISchema createSchema(ISchemaDescriptor descriptor, String location) {
							// TODO if the path return null we should search inside the bundle target
							// platform for the schema!
							return pathSchemaProvider.createSchema(descriptor, schemaLocation);
						}
					});
					schema = (Schema) desc.getSchema(false);

					// Check that all included schemas are available
					ISchemaInclude[] includes = schema.getIncludes();
					for (ISchemaInclude include : includes) {
						ISchema includedSchema = include.getIncludedSchema();
						if (includedSchema == null) {
							result.addError(NLS.bind(PDECoreMessages.ConvertSchemaToHTML_CannotFindIncludedSchema,
									include.getLocation(), schemaFile));
						}
					}

					File directory = destination;
					if (!directory.exists() || !directory.isDirectory()) {
						if (!directory.mkdirs()) {
							schema.dispose();
							return result;
						}
					}

					String id = extPoint.getId();
					if (id.indexOf('.') == -1) {
						id = pluginID + "." + id; //$NON-NLS-1$
					}
					File file = new File(directory, id.replace('.', '_') + ".html"); //$NON-NLS-1$
					try (PrintWriter out = new PrintWriter(
							Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8), true)) {
						fTransformer.transform(schema, out, cssURL, SchemaTransformer.BUILD);
					}
				} finally {
					if (schema != null) {
						schema.dispose();
					}
				}
			}
		}
		return result;
	}

	private String getPluginID(File manifest) {
		File OSGiFile = new File(manifest.getParentFile(), ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);

		if (OSGiFile.exists()) {
			try (FileInputStream manifestStream = new FileInputStream(OSGiFile)) {
				Map<String, String> headers = ManifestElement.parseBundleManifest(manifestStream, new HashMap<>());
				String value = headers.get(Constants.BUNDLE_SYMBOLICNAME);
				if (value == null) {
					return null;
				}
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, value);
				if (elements.length > 0) {
					return elements[0].getValue();
				}
			} catch (Exception e1) {
				System.out.print(e1.getMessage());
			}
		}
		return null;
	}

	private IPluginModelBase readManifestFile(File manifest) throws IOException, CoreException {
		try (InputStream stream = new BufferedInputStream(new FileInputStream(manifest))) {
			ExternalPluginModelBase model = null;
			switch (manifest.getName().toLowerCase(Locale.ENGLISH)) {
			case ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR:
				model = new ExternalFragmentModel();
				break;
			case ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR:
				model = new ExternalPluginModel();
				break;
			default:
				stream.close();
				throw new IOException(NLS.bind(PDECoreMessages.Builders_Convert_illegalValue, "manifest")); //$NON-NLS-1$
			}
			String parentPath = manifest.getParentFile().getAbsolutePath();
			model.setInstallLocation(parentPath);
			model.load(stream, false);
			stream.close();
			return model;
		}
	}
}
