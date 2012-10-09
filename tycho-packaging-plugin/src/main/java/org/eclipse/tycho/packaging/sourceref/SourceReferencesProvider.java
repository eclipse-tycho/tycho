/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging.sourceref;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.util.ManifestElement;

/**
 * Provides the value of Eclipse-SourceReferences header [1] for a project with a given SCM URL of a
 * maven SCM URL [2]. Implementations are plexus components which must declare their associated SCM
 * type as role hint and are selected based on the matching type of the given SCM URL.
 * 
 * [1] {@linkplain http://wiki.eclipse.org/PDE/UI/SourceReferences}
 * 
 * [2] {@linkplain http://maven.apache.org/scm/scm-url-format.html}
 */
public interface SourceReferencesProvider {

    /**
     * 
     * Provides the Eclipse-SourceReferences MANIFEST header value for the given maven project with
     * given SCM URL.
     * 
     * @param project
     *            the project for which to calculate the header
     * @param scmUrl
     *            the SCM URL of the project
     * 
     * @return the header value. Must be a parseable value as defined by
     *         {@link ManifestElement#parseHeader(String, String)}
     * @throws MojoExecutionException
     */
    public String getSourceReferencesHeader(MavenProject project, ScmUrl scmUrl) throws MojoExecutionException;

}
