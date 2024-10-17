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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.tycho.core.VerifierService;
import org.eclipse.tycho.core.maven.AbstractP2Mojo;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * <p>
 * Checks the consistency of the aggregated p2 repository.
 * </p>
 * 
 */
@Mojo(name = "verify-repository", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class VerifyIntegrityRepositoryMojo extends AbstractP2Mojo {
    private static final Object LOCK = new Object();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private VerifierService verifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            File repositoryDir = getBuildDirectory().getChild("repository");
            logger.info("Verifying p2 repositories in " + repositoryDir);
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
}
