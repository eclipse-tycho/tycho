/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts.configuration;

import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithVersion;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithVersionRange;
import static org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityPattern.patternWithoutVersion;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.artifacts.TargetPlatformFilter;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.CapabilityType;
import org.eclipse.tycho.artifacts.TargetPlatformFilter.FilterAction;
import org.eclipse.tycho.artifacts.TargetPlatformFilterSyntaxException;
import org.eclipse.tycho.core.test.utils.ResourceUtil;
import org.eclipse.tycho.core.utils.TychoVersion;

public class TargetPlatformFilterConfigurationReaderTest extends PlexusTestCase {

    private TargetPlatformFilterConfigurationReader subject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        subject = new TargetPlatformFilterConfigurationReader();
    }

    public void testMissingTypeException() throws Exception {
        try {
            Xpp3Dom filterConfig = getTargetFilters("targetfilter/missing_scope_type/pom.xml");
            subject.parseFilterConfiguration(filterConfig);
            fail();
        } catch (TargetPlatformFilterSyntaxException e) {
        }
    }

    public void testMissingIdException() throws Exception {
        try {
            Xpp3Dom filterConfig = getTargetFilters("targetfilter/missing_scope_id/pom.xml");
            subject.parseFilterConfiguration(filterConfig);
            fail();
        } catch (TargetPlatformFilterSyntaxException e) {
        }
    }

    public void testMissingActionException() throws Exception {
        try {
            Xpp3Dom filterConfig = getTargetFilters("targetfilter/missing_action/pom.xml");
            subject.parseFilterConfiguration(filterConfig);
            fail();
        } catch (TargetPlatformFilterSyntaxException e) {
        }
    }

    public void testValidRemoveAllFilters() throws Exception {
        Xpp3Dom filterConfig = getTargetFilters("targetfilter/valid_removeAll/pom.xml");
        List<TargetPlatformFilter> filters = subject.parseFilterConfiguration(filterConfig);

        for (TargetPlatformFilter filter : filters) {
            assertThat(filter.getAction(), is(FilterAction.REMOVE_ALL));
        }

        assertThat(
                filters.get(0).getScopePattern(),
                is(patternWithoutVersion(CapabilityType.OSGI_BUNDLE,
                        "org.eclipse.equinox.servletbridge.extensionbundle")));
        assertThat(filters.get(1).getScopePattern(),
                is(patternWithVersionRange(CapabilityType.OSGI_BUNDLE, "org.eclipse.equinox.app", "[3.6.0,3.6.1)")));
        assertThat(filters.get(2).getScopePattern(),
                is(patternWithVersion(CapabilityType.P2_INSTALLABLE_UNIT, "a.jre.javase", "1.7.0")));
    }

    public void testDuplicateVersionException() throws Exception {
        try {
            Xpp3Dom filterConfig = getTargetFilters("targetfilter/duplicate_scope_version/pom.xml");
            subject.parseFilterConfiguration(filterConfig);
            fail();
        } catch (TargetPlatformFilterSyntaxException e) {
        }
    }

    public void testValidRestrictToFilters() throws Exception {
        Xpp3Dom filterConfig = getTargetFilters("targetfilter/valid_restrictTo/pom.xml");
        List<TargetPlatformFilter> filters = subject.parseFilterConfiguration(filterConfig);

        for (TargetPlatformFilter filter : filters) {
            assertThat(filter.getAction(), is(FilterAction.RESTRICT));
        }

        assertThat(filters.get(0).getScopePattern(),
                is(patternWithoutVersion(CapabilityType.OSGI_BUNDLE, "org.eclipse.osgi")));
        assertThat(filters.get(0).getActionPattern(), is(patternWithVersionRange(null, null, "[3.6,3.7)")));

        assertThat(filters.get(1).getScopePattern(),
                is(patternWithoutVersion(CapabilityType.OSGI_BUNDLE, "org.eclipse.osgi")));
        assertThat(filters.get(1).getActionPattern(),
                is(patternWithVersionRange(CapabilityType.OSGI_BUNDLE, "org.eclipse.osgi", "[3.6,3.7)")));

        assertThat(filters.get(2).getScopePattern(),
                is(patternWithoutVersion(CapabilityType.JAVA_PACKAGE, "javax.persistence")));
        assertThat(filters.get(2).getActionPattern(),
                is(patternWithVersionRange(CapabilityType.OSGI_BUNDLE, "javax.persistence", "2.0")));

        assertThat(filters.get(3).getScopePattern(),
                is(patternWithVersionRange(CapabilityType.OSGI_BUNDLE, "org.eclipse.equinox.app", "[3.6.0,3.7.0)")));
        assertThat(filters.get(3).getActionPattern(), is(patternWithVersion(null, null, "3.6.2.v00000000")));

        assertThat(filters.get(4).getScopePattern(),
                is(patternWithoutVersion(CapabilityType.P2_INSTALLABLE_UNIT, "a.jre.javase")));
        assertThat(filters.get(4).getActionPattern(), is(patternWithVersion(null, null, "1.5.0")));
    }

    public void testDuplicateActionException() throws Exception {
        try {
            Xpp3Dom filterConfig = getTargetFilters("targetfilter/duplicate_action/pom.xml");
            subject.parseFilterConfiguration(filterConfig);
            fail();
        } catch (TargetPlatformFilterSyntaxException e) {
        }
    }

    private Xpp3Dom getTargetFilters(String pomFile) throws IOException, Exception, ProjectBuildingException {
        File pom = ResourceUtil.resourceFile(pomFile);
        Xpp3Dom config = getTargetPlatformConfiguration(pom);
        return config.getChild("filters");
    }

    // TODO 356579 error cases:
    // - restrict without attribute 
    //   -> did you mean restrictTo.version if scope version is set
    //   -> did you mean removeAll if no scope version is set

    Xpp3Dom getTargetPlatformConfiguration(File pom) throws Exception, ProjectBuildingException {
        MavenProject project = buildProjectModel(pom);
        return getPluginConfiguration(project, "target-platform-configuration");
    }

    // TODO share this?
    MavenProject buildProjectModel(File pom) throws Exception, ProjectBuildingException {
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        List<ProjectBuildingResult> projects = projectBuilder.build(Collections.singletonList(pom), false,
                projectBuildRequestForUnitTest());
        MavenProject project = projects.get(0).getProject();
        return project;
    }

    DefaultProjectBuildingRequest projectBuildRequestForUnitTest() {
        DefaultProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest();

        Properties userProperties = new Properties();
        userProperties.put("tycho-version", TychoVersion.getTychoVersion());
        projectBuildingRequest.setUserProperties(userProperties);

        // this disables the expansion of packaging types (which are undefined at this point in the build)
        projectBuildingRequest.setProcessPlugins(false);

        return projectBuildingRequest;
    }

    Xpp3Dom getPluginConfiguration(MavenProject project, String artifactId) {
        List<Plugin> plugins = project.getBuild().getPlugins();
        for (Plugin plugin : plugins) {
            if (artifactId.equals(plugin.getArtifactId())) {
                Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();
                return config;
            }
        }
        return null;
    }
}
