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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public abstract class AbstractGpgMojoExtension extends AbstractGpgMojo {

    @Override
    protected ProxySignerWithPublicKeyAccess newSigner(MavenProject project) throws MojoFailureException {
        return new ProxySignerWithPublicKeyAccess(super.newSigner(project), getSigner(), getPGPInfo(), getSecretKeys());
    }

    @Override
    protected AbstractGpgSigner createSigner(String name) throws MojoFailureException {
        //due to legacy reasons we actually used a GpgSigner as a delegate
        //(see org.apache.maven.plugins.gpg.ProxySignerWithPublicKeyAccess.getSigner(File, File))
        //it would be better to actually create the BouncyCastleSigner already here!
        return super.createSigner(GpgSigner.NAME);
    }

    protected String getSigner() {
        return "gpg";
    }

    protected File getPGPInfo() {
        return null;
    }

    protected File getSecretKeys() {
        return null;
    }
}
