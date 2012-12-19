/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.source;

import static java.util.Arrays.asList;

import java.io.File;

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;

public class OsgiSourceMojoTest extends PlexusTestCase {

    private OsgiSourceMojo mojo;

    @Override
    protected void setUp() throws Exception {
        BuildPropertiesParser parser = lookup(BuildPropertiesParser.class);
        mojo = new OsgiSourceMojo();
        mojo.buildPropertiesParser = parser;
    }

    public void testIsRelevantProjectPackagingType() throws Exception {
        assertTrue(mojo.isRelevantProject(createStubProjectWithSourceFolder("eclipse-plugin")));
        assertTrue(mojo.isRelevantProject(createStubProjectWithSourceFolder("eclipse-test-plugin")));
        assertFalse(mojo.isRelevantProject(createStubProjectWithSourceFolder("foo")));
    }

    public void testIsRelevantProjectSourcePluginEnabled() throws Exception {
        assertTrue(mojo.isRelevantProject(createStubProjectWithSourceFolder(true)));
        assertFalse(mojo.isRelevantProject(createStubProjectWithSourceFolder(false)));
    }

    public void testIsRelevantProjectWithSourceIncludesOnly() throws Exception {
        MavenProject stubProject = createStubProject("eclipse-plugin", "srcIncludesOnly", true);
        assertTrue(mojo.isRelevantProject(stubProject));
    }

    public void testIsRelevantProjectNoSources() throws Exception {
        MavenProject stubProject = createStubProject("eclipse-plugin", "noSources", true);
        assertFalse(mojo.isRelevantProject(stubProject));
    }

    public void testIsRelevantProjectRequireSourceRootsConfigured() throws Exception {
        MavenProject stubProject = createStubProject("eclipse-plugin", "noSources", true, true);
        assertTrue(mojo.isRelevantProject(stubProject));
    }

    private MavenProject createStubProjectWithSourceFolder(String packaging) {
        return createStubProject(packaging, "srcFolder", true);
    }

    private MavenProject createStubProjectWithSourceFolder(boolean sourcePluginEnabled) {
        return createStubProject("eclipse-plugin", "srcFolder", sourcePluginEnabled);
    }

    private MavenProject createStubProject(String packaging, String testResourceFolder, boolean enableSourePlugin) {
        return createStubProject(packaging, testResourceFolder, enableSourePlugin, false);
    }

    private MavenProject createStubProject(String packaging, String testResourceFolder, boolean enableSourePlugin,
            boolean requireSourceRoots) {
        MavenProject stubProject = new MavenProject();
        stubProject.setPackaging(packaging);
        if (enableSourePlugin) {
            Build build = new Build();
            stubProject.setBuild(build);
            Plugin tychoSourcePlugin = new Plugin();
            tychoSourcePlugin.setGroupId("org.eclipse.tycho");
            tychoSourcePlugin.setArtifactId("tycho-source-plugin");
            PluginExecution execution = new PluginExecution();
            execution.setGoals(asList("plugin-source"));
            if (requireSourceRoots) {
                Xpp3Dom config = new Xpp3Dom("configuration");
                Xpp3Dom requireSourceRootsDom = new Xpp3Dom("requireSourceRoots");
                requireSourceRootsDom.setValue("true");
                config.addChild(requireSourceRootsDom);
                execution.setConfiguration(config);
            }
            tychoSourcePlugin.setExecutions(asList(execution));
            build.setPlugins(asList(tychoSourcePlugin));
        }
        stubProject.setFile(new File("src/test/resources/sourceMojo/" + testResourceFolder + "/pom.xml"));
        return stubProject;
    }

}
