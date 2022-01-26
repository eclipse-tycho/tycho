/*******************************************************************************
 * Copyright (c) 2015, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.source;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.osgi.framework.Constants;

public class OsgiSourceMojoTest extends AbstractTychoMojoTestCase {

    private OsgiSourceMojo mojo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mojo = new OsgiSourceMojo();
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

    public void testDefaultClassifier() throws Exception {
        File basedir = getBasedir("bundle01");
        List<MavenProject> projects = getSortedProjects(basedir, basedir);

        MavenSession session = newMavenSession(projects.get(0));
        OsgiSourceMojo sourceMojo = (OsgiSourceMojo) lookupMojoWithDefaultConfiguration(projects.get(0), session,
                "plugin-source");

        assertEquals(ReactorProject.SOURCE_ARTIFACT_CLASSIFIER, sourceMojo.getClassifier());
    }

    public void testCustomClassifier() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put("classifier", "otherclassifier");
        OsgiSourceMojo sourceMojo = (OsgiSourceMojo) lookupMojo("org.eclipse.tycho", "tycho-source-plugin",
                TychoVersion.getTychoVersion(), "plugin-source", null);
        setVariableValueToObject(sourceMojo, "classifier", "customclassifier");
        assertEquals("customclassifier", sourceMojo.getClassifier());
    }

    public void testReadL10nPropsWithExistingL10nFile() throws Exception {
        readL10nPropsSetup();
        OsgiManifest manifest = mock(OsgiManifest.class);
        when(manifest.getValue(Constants.BUNDLE_LOCALIZATION)).thenReturn("l10n");
        Properties properties = mojo.readL10nProps(manifest);
        verify(mojo.getLog(), never()).warn(anyString());
        assertNotNull(properties);
    }

    public void testReadL10nPropsShouldPrintWarningForNonExistingL10NFile() throws Exception {
        readL10nPropsSetup();
        OsgiManifest manifest = mock(OsgiManifest.class);
        when(manifest.getValue(Constants.BUNDLE_LOCALIZATION)).thenReturn("nonexisting");
        Properties properties = mojo.readL10nProps(manifest);
        verify(mojo.getLog(), times(1)).warn(anyString());
        assertNull(properties);
    }

    public void testReadL10nPropsShouldNotWarnIfBundleIsNotL10Ned() throws Exception {
        readL10nPropsSetup();
        OsgiManifest manifest = mock(OsgiManifest.class);
        when(manifest.getValue(Constants.BUNDLE_LOCALIZATION)).thenReturn(null);
        Properties properties = mojo.readL10nProps(manifest);
        verify(mojo.getLog(), never()).warn(anyString());
        assertNull(properties);
    }

    public void readL10nPropsSetup() throws Exception {
        File basedir = getBasedir("bundleWithL10N");
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File(basedir, "pom.xml"));
        mojo.project = mavenProject;
        Log log = mock(Log.class);
        mojo.setLog(log);
    }

    private MavenProject createStubProjectWithSourceFolder(String packaging) throws ComponentLookupException {
        return createStubProject(packaging, "srcFolder", true);
    }

    private MavenProject createStubProjectWithSourceFolder(boolean sourcePluginEnabled)
            throws ComponentLookupException {
        return createStubProject("eclipse-plugin", "srcFolder", sourcePluginEnabled);
    }

    private MavenProject createStubProject(String packaging, String testResourceFolder, boolean enableSourePlugin)
            throws ComponentLookupException {
        return createStubProject(packaging, testResourceFolder, enableSourePlugin, false);
    }

    private MavenProject createStubProject(String packaging, String testResourceFolder, boolean enableSourePlugin,
            boolean requireSourceRoots) throws ComponentLookupException {
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
        ReactorProject project = DefaultReactorProject.adapt(stubProject);
        project.setContextValue(ReactorProject.CTX_BUILDPROPERTIESPARSER, lookup(BuildPropertiesParser.class));
        project.setContextValue(ReactorProject.CTX_INTERPOLATOR, new Interpolator() {

            @Override
            public String interpolate(String input) {
                return input;
            }
        });
        stubProject.setFile(new File("src/test/resources/sourceMojo/" + testResourceFolder + "/pom.xml"));
        return stubProject;
    }

}
