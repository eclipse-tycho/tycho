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
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.osgi.framework.BundleException;

public class EquinoxResolverTest extends AbstractTychoMojoTestCase {

    private EquinoxResolver subject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        subject = lookup(EquinoxResolver.class);
    }

    @Override
    protected void tearDown() throws Exception {
        subject = null;

        super.tearDown();
    }

    public void test_noSystemBundle() throws BundleException {
        Properties properties = subject.getPlatformProperties(new Properties(), null, null);
        State state = subject.newState(new DefaultTargetPlatform(), properties);

        BundleDescription[] bundles = state.getBundles("system.bundle");

        assertEquals(1, bundles.length);
    }

    public void test_bundleRuntimeExecutionEnvironment() throws Exception {
        File basedir = getBasedir("projects/bree");

        Properties properties = new Properties();
        properties.put("tycho-version", TychoVersion.getTychoVersion());

        List<MavenProject> projects = getSortedProjects(basedir, properties, null);
        assertEquals(5, projects.size());

        assertEquals("executionenvironment.manifest", projects.get(1).getArtifactId());
        ExecutionEnvironment ee = TychoProjectUtils.getExecutionEnvironmentConfiguration(projects.get(1))
                .getFullSpecification();
        assertEquals("CDC-1.0/Foundation-1.0", ee.getProfileName());
        Properties platformProperties = subject.getPlatformProperties(projects.get(1), ee);
        assertEquals("javax.microedition.io", platformProperties.get(Constants.FRAMEWORK_SYSTEMPACKAGES));
    }
}
