/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.net.URI;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.verifier.facade.VerifierService;

/**
 * @goal verify-repository
 * 
 * @phase verify
 */
public class VerifyIntegrityRepositoryMojo extends AbstractRepositoryMojo implements LogEnabled {
    private Logger logger;

    /** @component */
    private EquinoxServiceFactory p2;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File repositoryDir = getBuildDirectory().getChild("repository");
        logger.info("Verifying p2 repositories in " + repositoryDir);
        VerifierService verifier = p2.getService(VerifierService.class);
        URI repositoryUri = repositoryDir.toURI();
        try {
            if (!verifier.verify(repositoryUri, repositoryUri, getBuildDirectory())) {
                throw new MojoFailureException("The repository is invalid.");
            }
        } catch (FacadeException e) {
            throw new MojoExecutionException("Verification failed", e);
        }
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }
}
