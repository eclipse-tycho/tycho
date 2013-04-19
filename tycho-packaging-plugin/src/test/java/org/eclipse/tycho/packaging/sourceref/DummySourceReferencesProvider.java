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

/**
 * For test purposes only
 */
public class DummySourceReferencesProvider implements SourceReferencesProvider {

    public String getSourceReferencesHeader(MavenProject project, ScmUrl scmUrl) throws MojoExecutionException {
        return scmUrl.getUrl() + ";path=\"dummy/path\"";
    }

}
