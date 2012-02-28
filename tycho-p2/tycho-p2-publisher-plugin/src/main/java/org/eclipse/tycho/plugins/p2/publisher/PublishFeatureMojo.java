/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;

/**
 * @goal publish-feature
 */
public class PublishFeatureMojo extends AbstractPublishMojo {

    @Override
    protected Collection<?> publishContent(PublisherService publisherService) throws MojoExecutionException,
            MojoFailureException {
        File featureJar = getProject().getArtifact().getFile();
        try {
            return publisherService.publishFeature(featureJar);
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing feature " + featureJar.getAbsolutePath(), e);
        }
    }
}
