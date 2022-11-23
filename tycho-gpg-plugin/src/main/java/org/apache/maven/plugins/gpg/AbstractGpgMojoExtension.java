/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.apache.maven.plugins.gpg;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public abstract class AbstractGpgMojoExtension extends AbstractGpgMojo {

    @Override
    protected ProxySignerWithPublicKeyAccess newSigner(MavenProject project)
            throws MojoExecutionException, MojoFailureException {
        return new ProxySignerWithPublicKeyAccess(super.newSigner(project), getSigner(), getPGPInfo());
    }

    protected String getSigner() {
        return "gpg";
    }

    protected File getPGPInfo() {
        return null;
    }
}
