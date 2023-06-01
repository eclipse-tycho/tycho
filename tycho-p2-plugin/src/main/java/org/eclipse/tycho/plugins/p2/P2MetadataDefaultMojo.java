/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
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
import org.apache.maven.plugins.annotations.Parameter;

/**
 * P2 metadata generation goal bound to default artifact build lifecycle. For explicit binding use
 * p2-metadata goal. The idea is to avoid double p2 metadata generation for projects that generate
 * additional artifacts or post process standard artifacts using custom goals bound to package
 * phase.
 */
@Mojo(name = "p2-metadata-default", threadSafe = true)
public class P2MetadataDefaultMojo extends P2MetadataMojo {
    @Parameter(defaultValue = "true")
    private boolean defaultP2Metadata;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (defaultP2Metadata) {
            super.execute();
        }
    }
}
