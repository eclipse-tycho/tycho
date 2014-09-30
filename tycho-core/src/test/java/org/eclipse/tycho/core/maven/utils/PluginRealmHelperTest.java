/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PluginRealmHelperTest {

    private PluginRealmHelper helper;
    private List<MavenProject> projects;

    @Before
    public void setup() {
        helper = new PluginRealmHelper();
        MavenProject project1 = new MavenProject();
        project1.setArtifactId("myArtifactId1");
        project1.setGroupId("myGroupId");
        project1.setVersion("1.0.0");

        MavenProject project2 = new MavenProject();
        project2.setArtifactId("myPluginId");
        project2.setGroupId("myGroupId");
        project2.setVersion("1.0.0");

        projects = new ArrayList<MavenProject>();
        projects.add(project1);
        projects.add(project2);
    }

    @Test
    public void testIsPluginPartOfProjectsReturnsTrue() {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("myPluginId");
        plugin.setGroupId("myGroupId");
        plugin.setVersion("1.0.0");
        Assert.assertTrue(helper.isPluginPartOfSession(plugin, projects));
    }

    @Test
    public void testIsPluginPartOfProjectsVersionsNotMatch() {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("myPluginId");
        plugin.setGroupId("myGroupId");
        plugin.setVersion("2.0.0");
        Assert.assertFalse(helper.isPluginPartOfSession(plugin, projects));
    }

    @Test
    public void testIsPluginPartOfProjectsArtifactIdNotMatch() {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("myOtherPluginId");
        plugin.setGroupId("myGroupId");
        plugin.setVersion("1.0.0");
        Assert.assertFalse(helper.isPluginPartOfSession(plugin, projects));
    }

    @Test
    public void testIsPluginPartOfProjectsGroupIdNotMatch() {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("myPluginId");
        plugin.setGroupId("myOtherGroupId");
        plugin.setVersion("1.0.0");
        Assert.assertFalse(helper.isPluginPartOfSession(plugin, projects));
    }

}
