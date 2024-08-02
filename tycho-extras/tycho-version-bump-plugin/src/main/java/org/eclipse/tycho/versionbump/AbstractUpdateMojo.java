/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *    Christoph Läubrich - Issue #502 - TargetDefinitionUtil / UpdateTargetMojo should not be allowed to modify the internal state of the target 
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.util.Collections;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.resolver.P2Resolver;
import org.eclipse.tycho.core.resolver.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

public abstract class AbstractUpdateMojo extends AbstractMojo {

    @Component
    private Logger logger;

    @Parameter(defaultValue = "JavaSE-1.7")
    protected String executionEnvironment;

    protected P2Resolver p2;

    protected TargetPlatformConfigurationStub resolutionContext;

    @Parameter(property = "project")
    protected MavenProject project;

    @Component
    P2ResolverFactory factory;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createResolver();
            doUpdate();
        } catch (Exception e) {
            if (e instanceof MojoFailureException mfe) {
                throw mfe;
            }
            if (e instanceof MojoExecutionException mee) {
                throw mee;
            }
            throw new MojoExecutionException("Could not update " + getFileToBeUpdated(), e);
        }
    }

    protected abstract File getFileToBeUpdated() throws MojoExecutionException, MojoFailureException;

    protected abstract void doUpdate() throws Exception;

    private void createResolver() {
        p2 = factory.createResolver(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));
        resolutionContext = new TargetPlatformConfigurationStub();
    }

}
