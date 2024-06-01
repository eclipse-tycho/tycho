/**********************************************************************
 * Copyright (c) 2006, 2010 Eclipse Foundation and others.
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

/**
 * Constants for the files usually manipulated by the fetch factory.
 * @since 3.2
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface Constants {
	/** Constant for the string <code>feature.xml</code> */
	public final static String FEATURE_FILENAME_DESCRIPTOR = "feature.xml"; //$NON-NLS-1$

	/** Constant for the string <code>fragment.xml</code> */
	public final static String FRAGMENT_FILENAME_DESCRIPTOR = "fragment.xml"; //$NON-NLS-1$

	/** Constant for the string <code>plugin.xml</code> */
	public final static String PLUGIN_FILENAME_DESCRIPTOR = "plugin.xml"; //$NON-NLS-1$

	/** Constant for the string <code>META-INF/MANIFEST.MF</code> */
	public final static String BUNDLE_FILENAME_DESCRIPTOR = "META-INF/MANIFEST.MF"; //$NON-NLS-1$

	/**
	 * Key used to store the value of a project reference in the Eclipse-SourceReferences manifest header.  
	 * 
	 * @since 3.6
	 * @see IFetchFactory
	 */
	public static final String KEY_SOURCE_REFERENCES = "sourceReferences"; //$NON-NLS-1$

}
