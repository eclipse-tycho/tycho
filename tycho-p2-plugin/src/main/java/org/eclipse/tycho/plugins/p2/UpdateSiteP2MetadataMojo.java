/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.equinox.internal.p2.updatesite.UpdateSitePublisherApplication;

@Mojo(name = "update-site-p2-metadata", threadSafe = true)
public class UpdateSiteP2MetadataMojo extends AbstractP2MetadataMojo {
    private static final Object LOCK = new Object();

    @Override
    protected UpdateSitePublisherApplication getPublisherApplication() {
        return new UpdateSitePublisherApplication();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        synchronized (LOCK) {
            super.execute();
        }
    }
}
