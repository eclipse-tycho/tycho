/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.core.TargetPlatform;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.osgitools.DependencyComputer;
import org.eclipse.tycho.core.osgitools.DependencyComputer.DependencyEntry;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.core.utils.MavenSessionUtils;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Assert;
import org.junit.Test;

public class DependencyComputerTest extends AbstractTychoMojoTestCase {
    private DependencyComputer dependencyComputer;

    protected void setUp() throws Exception {
        super.setUp();
        dependencyComputer = (DependencyComputer) lookup(DependencyComputer.class);
    }

    @Override
    protected void tearDown() throws Exception {
        dependencyComputer = null;
        super.tearDown();
    }

    @Test
    public void testExportPackage() throws Exception {
        File basedir = getBasedir("projects/exportpackage");
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);

        EquinoxResolver resolver = lookup(EquinoxResolver.class);

        Map<File, MavenProject> basedirMap = MavenSessionUtils.getBasedirMap(result.getTopologicallySortedProjects());

        MavenProject project = basedirMap.get(new File(basedir, "bundle"));
        TargetPlatform platform = (TargetPlatform) project.getContextValue(TychoConstants.CTX_TARGET_PLATFORM);

        State state = resolver.newResolvedState(project, platform);
        BundleDescription bundle = state.getBundleByLocation(project.getBasedir().getAbsolutePath());

        List<DependencyEntry> dependencies = dependencyComputer.computeDependencies(state.getStateHelper(), bundle);
        Assert.assertEquals(3, dependencies.size());
        Assert.assertEquals("dep", dependencies.get(0).desc.getSymbolicName());
        Assert.assertEquals("dep2", dependencies.get(1).desc.getSymbolicName());
        Assert.assertEquals("dep3", dependencies.get(2).desc.getSymbolicName());
        Assert.assertTrue(dependencies.get(2).rules.isEmpty());
    }

    @Test
    public void testTYCHO0378unwantedSelfDependency() throws Exception {
        File basedir = getBasedir("projects/TYCHO0378unwantedSelfDependency");
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);

        Assert.assertEquals(0, result.getProject().getDependencies().size());
    }
}
