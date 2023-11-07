/**********************************************************************
 * Copyright (c) 2004, 2019 Eclipse Foundation and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gunnar Wagenknecht - Initial API and implementation
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.build;

import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Interface to be implemented by clients of the <code>org.eclipse.pde.build.fetchFactories</code> extension-point.
 * <p>
 * The factories are being used at various points in the execution of the PDE Build <code>eclipse.fetch</code> Ant task.
 * Based on a map file entry, they are responsible for generating segments of an ant script whose execution will fetch 
 * plug-ins, fragments, bundles and features or individual files contained in one of those elements.
 * The format of a map file entry is:
 * <code>
 * &lt;elementType&gt;@&lt;elementName&gt; = &lt;repositoryTag&gt;, &lt;repositoryDetails&gt;
 * </code>
 * The format of <code>elementType</code> and <code>elementName</code> is fixed.
 * The factories specify the value of <code>repositoryTag</code> and the format of the <code>repositoryDetails</code>.
 * <code>repositoryTag</code> and <code>repositoryDetails</code> becomes defacto APIs. 
 * <br>
 * <code>repositoryTag</code> should match the factory id used when declaring the factory extension. For example, for the CVS the value is "CVS". 
 * <code>repositoryDetails</code> should contains enough details to allow the factory to generate a fetch script retrieving the element.
  * </p>
 * <p>
 * The fetch factories are being contributed through the <code>org.eclipse.pde.build.fetchFactories</code> 
 * extension-points.
 * </p>
 * <p>
 * Fetch factories will not be re-used between different PDE Build <code>eclipse.fetch</code> Ant tasks. Each task will create at
 * most one fetch factory instance for the duration of the task processing. This allows implementors to
 * build and maintain stateful information. Such information must be released in {@link #addTargets(IAntScript)} call.
 * </p>
 * @since 3.2
 */
public interface IFetchFactory {
	/** Key used to store the value of the element name. */
	public static final String KEY_ELEMENT_NAME = "element"; //$NON-NLS-1$

	/** Key used to store the value of the element type */
	public static final String KEY_ELEMENT_TYPE = "type"; //$NON-NLS-1$

	/** Key used to store the value of the tag that will be used to fetch the element.
	 * <p>
	 *  The grammar of the expected value is limited to:
	 *  <pre>
	 *  	 value::= (alpha|digit|'_'|'-')+
	 *      digit ::= [0..9]
	 *      alpha ::= [a..zA..Z]
	 * </pre>
	 *  */
	public static final String KEY_ELEMENT_TAG = "tag"; //$NON-NLS-1$

	/** One of the value for element type. See {@link #KEY_ELEMENT_TYPE}.*/
	public static final String ELEMENT_TYPE_BUNDLE = "bundle"; //$NON-NLS-1$

	/** One of the value for element type. See {@link #KEY_ELEMENT_TYPE}.*/
	public static final String ELEMENT_TYPE_FEATURE = "feature"; //$NON-NLS-1$

	/** One of the value for element type. See {@link #KEY_ELEMENT_TYPE}.*/
	public static final String ELEMENT_TYPE_FRAGMENT = "fragment"; //$NON-NLS-1$

	/** One of the value for element type. See {@link #KEY_ELEMENT_TYPE}.*/
	public static final String ELEMENT_TYPE_PLUGIN = "plugin"; //$NON-NLS-1$

	/**
	 * This method should parse / validate a mapfile entry and derive a corresponding
	 * key / value pair structure containing the relevant information.
	 * <p>
	 * The arguments specified in the map file are provided. The map with entry
	 * infos should be filled with provider specific information that is
	 * required in later processing to sucessfully generate the fetch script.
	 * </p>
	 * <p>
	 * Since 3.6, factories may optionally set the {@link Constants#KEY_SOURCE_REFERENCES} property in the entry infos map to support the inclusion 
	 * of source references in the bundle manifest using the Eclipse-SourceReferences header.
	 * </p>
	 * @param rawEntry the arguments as specified in the map file (may not be <code>null</code>).
	 * @param overrideTags a key / value containing all the override tags specified for all the repository (maybe <code>null</code> or empty). 
	 * The values of this map of this are read from the fetchTag property (see file scripts/templates/headless-build/build.properties). 
	 * @param entryInfos the map to store repository specific information derived from the rawEntry.This object is being passed as arguments to 
	 * the other methods of the factory. The factories are also expected to set {@link #KEY_ELEMENT_TAG} to indicate the tag that will be used 
	 * to fetch the element. This value is for example used to generate the "qualifier" value of a version number. 
	 * Note that {@link #KEY_ELEMENT_NAME} and {@link #KEY_ELEMENT_TYPE} are reserved entries whose values respectively 
	 * refer to the name of the element being fetched and its type.
	 * @throws CoreException if the rawEntry is incorrect.
	 */
	public void parseMapFileEntry(String rawEntry, Properties overrideTags, Map<String, Object> entryInfos) throws CoreException;

	/**
	 * Generates a segment of ant script whose execution will fetch the element (bundle, plug-in, fragment, feature) indicated in the entryInfos arguments.
	 *
	 * @param entryInfos the map that has been built in the {@link #parseMapFileEntry(String, Properties, Map)} method.
	 * This map contains the name and the type of the element  (resp. {@link #KEY_ELEMENT_NAME} and {@link #KEY_ELEMENT_TYPE}) to put in the destination.
	 * @param destination the destination where the element should be fetched to. For example, for a plug-in the <code>plugin.xml</code> file is expected
	 * to be in <code>destination/plugin.xml</code>. 
	 * @param script the script in which to generate the segments of ant script. It is not authorized to generate target declaration during this call.  
	 */
	public void generateRetrieveElementCall(Map<String, Object> entryInfos, IPath destination, IAntScript script);

	/**
	 * Generates a segment of ant script whose execution will fetch the specified file from the given element.
	 *
	 * @param entryInfos the map that has been built in the {@link #parseMapFileEntry(String, Properties, Map)} method.
	 * This map contains the name and the type of the element  (resp. {@link #KEY_ELEMENT_NAME} and {@link #KEY_ELEMENT_TYPE}) to put in the destination.
	 * @param destination the destination where the element should be fetched to. For example, for a plug-in the <code>plugin.xml</code> file is expected
	 * to be in <code>destination/plugin.xml</code>. 
	 * @param files the files to obtained for the specified element.
	 * @param script the script in which to generate the segments of ant script. It is not authorized to generate target declaration during this call.  
	 */
	public void generateRetrieveFilesCall(Map<String, Object> entryInfos, IPath destination, String[] files, IAntScript script);

	/**
	 * This methods give opportunities to the factory to generate target declaration or other Ant top level constructs in the script.
	 * The generated elements can be invoked from the ant scripts segments created in {@link #generateRetrieveElementCall(Map, IPath, IAntScript)} 
	 * and {@link #generateRetrieveFilesCall(Map, IPath, String[], IAntScript)}.
	 */
	public void addTargets(IAntScript script);
}
