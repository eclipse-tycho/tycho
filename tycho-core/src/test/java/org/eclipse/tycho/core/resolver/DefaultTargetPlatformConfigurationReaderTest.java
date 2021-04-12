/*******************************************************************************
 * Copyright (c) 2014, 2021 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *    Christoph Läubrich - Adjust to changed API
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.shared.BuildFailureException;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Test;

public class DefaultTargetPlatformConfigurationReaderTest extends AbstractTychoMojoTestCase {

    private DefaultTargetPlatformConfigurationReader configurationReader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        configurationReader = lookup(DefaultTargetPlatformConfigurationReader.class);
    }

    @Override
    protected void tearDown() throws Exception {
        configurationReader = null;
        super.tearDown();
    }

    @Test
    public void testExtraRequirementMissingVersionRange() throws Exception {
        Xpp3Dom dom = createConfigurationDom("type", "id");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage()
                    .contains("Element <versionRange> is missing in <extraRequirements><requirement> section."));
        }
    }

    @Test
    public void testExtraRequirementMissingType() throws Exception {
        Xpp3Dom dom = createConfigurationDom("id", "versionRange");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(
                    e.getMessage().contains("Element <type> is missing in <extraRequirements><requirement> section."));
        }
    }

    @Test
    public void testExtraRequirementId() throws Exception {
        Xpp3Dom dom = createConfigurationDom("type", "versionRange");
        try {
            configurationReader.readExtraRequirements(new TargetPlatformConfiguration(), dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains("Element <id> is missing in <extraRequirements><requirement> section."));
        }
    }

    @Test()
    public void testAddTargetWithValidMissingTargetDefinition() {
        Xpp3Dom dom = createGavConfiguration("myGroupId", "myArtifactId", "myVersion");
        MavenSession session = setupMockSession();
        TargetPlatformConfiguration configuration = new TargetPlatformConfiguration();
        try {
            configurationReader.addTargetArtifact(configuration, session, null, dom);
        } catch (MojoExecutionException e) {
            assertTrue(e.getMessage().contains("No target definition file(s) found in project"));
        }
    }

    @Test
    public void testAddTargetWithMissingVersionInTargetDefinition() throws MojoExecutionException {
        Xpp3Dom dom = createGavConfiguration("myGroupId", "myArtifactId", null);
        MavenSession session = setupMockSession();
        TargetPlatformConfiguration configuration = new TargetPlatformConfiguration();
        try {
            configurationReader.addTargetArtifact(configuration, session, null, dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains("The target artifact configuration is invalid"));
        }
    }

    @Test
    public void testAddTargetWithMissingGroupInTargetDefinition() throws MojoExecutionException {
        Xpp3Dom dom = createGavConfiguration(null, "myArtifactId", "myVersion");
        MavenSession session = setupMockSession();
        TargetPlatformConfiguration configuration = new TargetPlatformConfiguration();
        try {
            configurationReader.addTargetArtifact(configuration, session, null, dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains("The target artifact configuration is invalid"));
        }
    }

    @Test
    public void testAddTargetWithMissingArtifactIdInTargetDefinition() throws MojoExecutionException {
        Xpp3Dom dom = createGavConfiguration("myGroupId", null, "myVersion");
        MavenSession session = setupMockSession();
        TargetPlatformConfiguration configuration = new TargetPlatformConfiguration();
        try {
            configurationReader.addTargetArtifact(configuration, session, null, dom);
            fail();
        } catch (BuildFailureException e) {
            assertTrue(e.getMessage().contains("The target artifact configuration is invalid"));
        }
    }

    @Test
    public void testOptionalResolution() throws MojoExecutionException {
        Xpp3Dom dom = createConfigurationDom();
        Xpp3Dom res = new Xpp3Dom(DefaultTargetPlatformConfigurationReader.DEPENDENCY_RESOLUTION);
        Xpp3Dom opt = new Xpp3Dom(DefaultTargetPlatformConfigurationReader.OPTIONAL_DEPENDENCIES);
        opt.setValue("optional");
        res.addChild(opt);
        dom.addChild(res);
        try {
            configurationReader.readDependencyResolutionConfiguration(new TargetPlatformConfiguration(), dom);
        } catch (BuildFailureException e) {
            fail(e.getMessage());
        }
    }

    private MavenSession setupMockSession() {
        MavenSession session = mock(MavenSession.class);
        MavenProject project = mock(MavenProject.class);
        when(session.getProjects()).thenReturn(Arrays.asList(project));
        when(project.getGroupId()).thenReturn("myGroupId");
        when(project.getArtifactId()).thenReturn("myArtifactId");
        when(project.getVersion()).thenReturn("myVersion");
        when(project.getBasedir()).thenReturn(new File("/basedir/"));
        return session;
    }

    private Xpp3Dom createGavConfiguration(String groupId, String artifactId, String version) {
        Xpp3Dom dom = new Xpp3Dom("artifact");
        if (groupId != null) {
            Xpp3Dom group = new Xpp3Dom("groupId");
            group.setValue(groupId);
            dom.addChild(group);
        }
        if (artifactId != null) {
            Xpp3Dom artifact = new Xpp3Dom("artifactId");
            artifact.setValue(artifactId);
            dom.addChild(artifact);
        }
        if (version != null) {
            Xpp3Dom ver = new Xpp3Dom("version");
            ver.setValue(version);
            dom.addChild(ver);
        }
        return dom;
    }

    private Xpp3Dom createConfigurationDom(String... requirementChildren) {
        Xpp3Dom dom = new Xpp3Dom("configuration");
        Xpp3Dom extraRequirements = new Xpp3Dom("extraRequirements");
        Xpp3Dom requirement = new Xpp3Dom("requirement");
        extraRequirements.addChild(requirement);
        dom.addChild(extraRequirements);
        for (String requirementChild : requirementChildren) {
            requirement.addChild(new Xpp3Dom(requirementChild));
        }
        return dom;
    }

}
