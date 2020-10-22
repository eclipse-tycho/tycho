/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.shared;

import java.util.List;
import java.util.Map;

/**
 * Represents a PDE build.properties file as defined by
 * 
 * <pre>
 * {@link https://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.pde.doc.user/reference/pde_feature_generating_build.htm} .
 * </pre>
 * 
 * Note that not all keys defined by PDE are supported.
 * 
 */
// keep in sync with BuildProperties.md docs
public interface BuildProperties {

    public String getJavacSource();

    public String getJavacTarget();

    public String getJreCompilationProfile();

    public String getForceContextQualifier();

    public List<String> getBinIncludes();

    public List<String> getBinExcludes();

    public List<String> getSourceIncludes();

    public List<String> getSourceExcludes();

    public List<String> getJarsExtraClasspath();

    public List<String> getJarsCompileOrder();

    public Map<String, String> getJarToJavacDefaultEncodingMap();

    public Map<String, String> getJarToOutputFolderMap();

    /**
     * Custom manifests for nested jars
     */
    public Map<String, String> getJarToManifestMap();

    public Map<String, String> getRootEntries();

    public Map<String, List<String>> getJarToSourceFolderMap();

    public Map<String, List<String>> getJarToExtraClasspathMap();

    public boolean isRootFilesUseDefaultExcludes();

}
