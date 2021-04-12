/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
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

package org.eclipse.tycho.packaging.sourceref;

import java.util.Map;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.tycho.packaging.SourceReferences;

@Component(role = SourceReferenceComputer.class)
public class SourceReferenceComputer {

    private static final String MANIFEST_HEADER = "Eclipse-SourceReferences";

    @Requirement
    private Map<String, SourceReferencesProvider> providerMap;

    public SourceReferenceComputer() {
    }

    public void addSourceReferenceHeader(Manifest manifest, SourceReferences sourceRefsConfiguration,
            MavenProject project) throws MojoExecutionException {
        if (!sourceRefsConfiguration.shouldGenerate()) {
            return;
        }
        if (sourceRefsConfiguration.getCustomValue() != null) {
            addSourceReferencesHeader(manifest, sourceRefsConfiguration.getCustomValue());
            return;
        }
        ScmUrl scmUrl = new ScmUrl(project.getProperties());
        final String providerHint = scmUrl.getType();
        SourceReferencesProvider provider = providerMap.get(providerHint);
        if (provider == null) {
            throw new MojoExecutionException("No source references provider for SCM type '" + providerHint
                    + "' registered.");
        }
        addSourceReferencesHeader(manifest, provider.getSourceReferencesHeader(project, scmUrl));
    }

    private static void addSourceReferencesHeader(Manifest manifest, String value) {
        manifest.getMainAttributes().putValue(MANIFEST_HEADER, value);
    }
}
