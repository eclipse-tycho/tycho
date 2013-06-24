/*******************************************************************************
 * Copyright (c) 2010, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;

public abstract class AbstractUpdateMojo extends AbstractMojo {

    /** @component */
    protected EquinoxServiceFactory equinox;

    /** @component */
    private Logger logger;

    /** @parameter default-value="JavaSE-1.6" */
    private String executionEnvironment;

    protected P2Resolver p2;

    protected TargetPlatformBuilder resolutionContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createResolver();
            doUpdate();
        } catch (Exception e) {
            throw new MojoExecutionException("Could not update " + getTargetFile().getAbsolutePath(), e);
        }
    }

    protected abstract File getTargetFile();

    protected abstract void doUpdate() throws IOException, URISyntaxException;

    private void createResolver() {
        P2ResolverFactory factory = equinox.getService(P2ResolverFactory.class);
        p2 = factory.createResolver(new MavenLoggerAdapter(logger, false));
        resolutionContext = factory.createTargetPlatformBuilder(new ExecutionEnvironmentConfigurationStub(
                executionEnvironment));
    }

}
