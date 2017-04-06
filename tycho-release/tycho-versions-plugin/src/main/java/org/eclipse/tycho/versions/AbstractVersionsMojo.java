/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour - Support for release-process like Maven
 *******************************************************************************/
package org.eclipse.tycho.versions;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

abstract class AbstractVersionsMojo extends AbstractMojo {

    @Parameter(property = "session", readonly = true)
    protected MavenSession session;

    @Component
    private PlexusContainer plexus;

    protected <T> T lookup(Class<T> clazz) throws MojoFailureException {
        try {
            return plexus.lookup(clazz);
        } catch (ComponentLookupException e) {
            throw new MojoFailureException("Could not lookup required component", e);
        }
    }

    protected ProjectMetadataReader newProjectMetadataReader() throws MojoFailureException {
        return lookup(ProjectMetadataReader.class);
    }

}
