/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * P2 metadata generation goal bound to default artifact build lifecycle. For explicit binding use
 * p2-metadata goal. The idea is to avoid double p2 metadata generation for projects that generate
 * additional artifacts or post process standard artifacts using custom goals bound to package
 * phase.
 * 
 * @goal p2-metadata-default
 */
public class P2MetadataDefaultMojo extends P2MetadataMojo {

    /**
     * @parameter default-value="true"
     */
    private boolean defaultP2Metadata;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (defaultP2Metadata) {
            super.execute();
        }
    }
}
