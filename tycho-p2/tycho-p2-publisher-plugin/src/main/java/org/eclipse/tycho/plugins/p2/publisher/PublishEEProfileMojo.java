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
 * @phase prepare-package
 */
public final class PublishEEProfileMojo extends AbstractPublishMojo {

    /**
     * The profile file containing the execution environment definition.
     * 
     * @parameter
     * @required
     */
    private File profileFile;

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        try {
            Collection<?> ius = publisherService.publishEEProfile(profileFile);
            getLog().info("Published profile IUs: " + ius);
            return ius;
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing execution environment profile " + profileFile
                    + ": " + e.getMessage(), e);
        }
    }
}
