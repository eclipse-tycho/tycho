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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.osgi.TychoServiceFactory;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

public abstract class AbstractUpdateMojo extends AbstractMojo {

	@Component(hint = TychoServiceFactory.HINT)
    protected EquinoxServiceFactory equinox;

    @Component
    private Logger logger;

    @Parameter(defaultValue = "JavaSE-1.7")
    protected String executionEnvironment;

    protected P2Resolver p2;

    protected TargetPlatformConfigurationStub resolutionContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createResolver();
            doUpdate();
        } catch (Exception e) {
            throw new MojoExecutionException("Could not update " + getFileToBeUpdated(), e);
        }
    }

    protected abstract File getFileToBeUpdated();

    protected abstract void doUpdate() throws Exception;

    private void createResolver() {
        P2ResolverFactory factory = equinox.getService(P2ResolverFactory.class);
        p2 = factory.createResolver(new MavenLoggerAdapter(logger, false));
        resolutionContext = new TargetPlatformConfigurationStub();
    }

}
