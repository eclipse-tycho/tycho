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
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;

/**
 * This goal publishes an execution environment profile
 * 
 * @goal publish-ee-profile
 */
public final class PublishEEProfileMojo extends AbstractPublishMojo {

    /**
     * The profile file containing the execution environment definition.
     * 
     * @parameter
     */
    // TODO support unset? -> use <artifactId>.profile
    private File profileFile;

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        try {
            // TODO assert that profile file is in the root!?
            // TODO copy profile file to the target folder because the JREAction may otherwise assemble the target folder
            Collection<?> ius = publisherService.publishEEProfile(profileFile);
            return ius;
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing execution environment profile " + profileFile
                    + ": " + e.getMessage(), e);
        }
    }
}
