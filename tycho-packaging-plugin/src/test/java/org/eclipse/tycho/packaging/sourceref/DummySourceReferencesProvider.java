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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

/**
 * For test purposes only
 */
@Component(role = SourceReferencesProvider.class, hint = "dummy")
public class DummySourceReferencesProvider implements SourceReferencesProvider {

    @Override
    public String getSourceReferencesHeader(MavenProject project, ScmUrl scmUrl) throws MojoExecutionException {
        return scmUrl.getUrl() + ";path=\"dummy/path\"";
    }

}
