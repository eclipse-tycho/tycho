/*******************************************************************************
 * Copyright (c) 2011 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.repository;

import java.io.File;
import java.net.URI;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.verifier.facade.VerifierService;

/**
 * <p>
 * Checks the consistency of the aggregated p2 repository.
 * </p>
 * 
 */
@Mojo(name = "verify-repository", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class VerifyIntegrityRepositoryMojo extends AbstractP2Mojo implements LogEnabled {
    private static final Object LOCK = new Object();
    private Logger logger;

    @Component(hint = TychoServiceFactory.HINT)
    private EquinoxServiceFactory p2;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
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
    }

    @Override
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }
}
