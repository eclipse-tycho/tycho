/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc (mistria) - initial implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.baseline;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.plugins.p2.BaselineMode;
import org.eclipse.tycho.plugins.p2.BaselineReplace;
import org.eclipse.tycho.plugins.p2.BaselineValidator;
import org.eclipse.tycho.plugins.p2.Repository;

/**
 * A strategy skipping baseline validation.
 * 
 */
@Component(role = BaselineValidator.class, hint = NoBaselineValidation.HINT)
public class NoBaselineValidation implements BaselineValidator {

    public static final String HINT = "none";

    public Map<String, IP2Artifact> validateAndReplace(MavenProject project, Map<String, IP2Artifact> reactorMetadata,
            List<Repository> baselineRepositories, BaselineMode baselineMode, BaselineReplace baselineReplace)
            throws IOException, MojoExecutionException {
        return reactorMetadata;
    }

}
