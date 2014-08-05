/*******************************************************************************
 * Copyright (c) 2015 Rapicorp, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Rapicorp, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.IU;

/**
 * Validate that the IU has a capability representing itself and validate that it has the property
 * org.eclipse.equinox.p2.type.iu set to true.
 */
@Mojo(name = "validate-iu", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateIUMojo extends AbstractVersionMojo {
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "project.packaging", required = true, readonly = true)
    protected String packaging;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!PackagingType.TYPE_P2_IU.equals(project.getPackaging()))
            return;
        IU iu = IU.loadIU(project.getBasedir());
        if (!iu.hasP2IUProperty())
            throw new MojoExecutionException(
                    "Property identifying the IU not found. Add the following to the p2iu.xml <property name='org.eclipse.equinox.p2.type.iu' value ='true'/>");
    }
}
