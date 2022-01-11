/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultDependencyArtifacts;
import org.eclipse.tycho.core.utils.TychoProjectUtils;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class BuildQualifierTest extends AbstractTychoMojoTestCase {

    private static final String BUILD_QUALIFIER_PROPERTY = "buildQualifier";
    private static final String UNQUALIFIED_VERSION_PROPERTY = "unqualifiedVersion";
    private static final String QUALIFIED_VERSION_PROPERTY = "qualifiedVersion";

    public void testForceContextQualifier() throws Exception {
        /*
         * This test covers all scenarios that involve forceContextQualifier mojo parameter, i.e.
         * setting -DforceContextQualifier=... on cli, specifying forceContextQualifier project
         * property and setting forceContextQualifier using explicit mojo configuration.
         */

        MavenProject project = getProject("projects/buildqualifier", "p001/pom.xml");
        project.getProperties().put(BUILD_QUALIFIER_PROPERTY, "garbage");
        BuildQualifierMojo mojo = createMojoWithProject(project);

        setVariableValueToObject(mojo, "forceContextQualifier", "foo-bar");

        mojo.execute();

        assertEquals("foo-bar", project.getProperties().get(BUILD_QUALIFIER_PROPERTY));
    }

    public void testBuildProperties() throws Exception {
        MavenProject project = getProject("projects/buildqualifier", "p002/pom.xml");
        project.getProperties().put(BUILD_QUALIFIER_PROPERTY, "garbage");
        BuildQualifierMojo mojo = createMojoWithProject(project);

        mojo.execute();

        assertEquals("blah", project.getProperties().get(BUILD_QUALIFIER_PROPERTY));
    }

    public void testTimestamp() throws Exception {

        MavenProject project = getProject("projects/buildqualifier", "p001/pom.xml");
        project.getProperties().put(BUILD_QUALIFIER_PROPERTY, "garbage");

        ArrayList<MavenProject> projects = new ArrayList<>();
        projects.add(project);

        BuildQualifierMojo mojo = createMojoWithProject(project);

        mojo.execute();

        String firstTimestamp = (String) project.getProperties().get(BUILD_QUALIFIER_PROPERTY);

        // lets do it again
        Thread.sleep(500L);

        project = getProject("projects/buildqualifier", "p001/pom.xml");
        assertNull(project.getProperties().get(BUILD_QUALIFIER_PROPERTY));
        mojo = createMojoWithProject(project);
        mojo.execute();

        String secondTimestamp = (String) project.getProperties().get(BUILD_QUALIFIER_PROPERTY);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        Date firstDate = dateFormat.parse(firstTimestamp);
        Date secondDate = dateFormat.parse(secondTimestamp);

        // 2nd build time must be the same (within the same minute)
        // or higher than the 1st build time
        assertTrue(secondDate.compareTo(firstDate) >= 0);
    }

    public void testUnqualifiedVersion() throws Exception {
        MavenProject project = getProject("projects/buildqualifier", "p002/pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);

        mojo.execute();

        assertEquals("0.0.1", project.getProperties().get(UNQUALIFIED_VERSION_PROPERTY));
    }

    public void testFullyQualifiedVersion() throws Exception {
        MavenProject project = getProject("projects/buildqualifier/fullyqualified", "pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);

        mojo.execute();

        assertEquals("0.0.1", project.getProperties().get(UNQUALIFIED_VERSION_PROPERTY));
        assertEquals("123456", project.getProperties().get(BUILD_QUALIFIER_PROPERTY));
        assertEquals("0.0.1.123456", project.getProperties().get(QUALIFIED_VERSION_PROPERTY));
    }

    public void testNoQualifiedVersion() throws Exception {
        MavenProject project = getProject("projects/buildqualifier/noqualifier", "pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);

        mojo.execute();

        assertEquals("0.0.1", project.getProperties().get(UNQUALIFIED_VERSION_PROPERTY));
        assertEquals("", project.getProperties().get(BUILD_QUALIFIER_PROPERTY));
        assertEquals("0.0.1", project.getProperties().get(QUALIFIED_VERSION_PROPERTY));
    }

    public void testTimeZone() {
        final TimeZone oldTimeZone = TimeZone.getDefault();
        try {
            final Date date = new Date(0L);
            String qualiferCreatedInGMTTimeZone = createTimeStampInTimeZone("GMT", date);
            String qualiferCreatedInGMTPlus2TimeZone = createTimeStampInTimeZone("GMT+02:00", date);
            assertEquals(qualiferCreatedInGMTPlus2TimeZone, qualiferCreatedInGMTTimeZone);
        } finally {
            TimeZone.setDefault(oldTimeZone);
        }
    }

    public void testStableQualifier() throws Exception {
        File basedir = getBasedir("projects/stablebuildqualifier/basic");

		List<MavenProject> projects = getSortedProjects(basedir);
        MavenSession session = newMavenSession(projects.get(0), projects);

        executeMojo(session, getProject(projects, "bundle01"));
        executeMojo(session, getProject(projects, "bundle02"));
        executeMojo(session, getProject(projects, "feature02"), "build-qualifier-aggregator");
        executeMojo(session, getProject(projects, "feature"), "build-qualifier-aggregator");
        executeMojo(session, getProject(projects, "product"), "build-qualifier-aggregator");

        assertQualifier("201205192000", projects, "bundle02");
        // feature02 includes bundle02, but its qualifier is hard-coded via the manifest
        assertQualifier("201205191300", projects, "feature02");
        // product includes feature02, and hence transitively also bundle02, but qualifier is only the max. of direct inclusions
        assertQualifier("201205191300", projects, "product");

        assertQualifier("201205191500", projects, "bundle01");
        // feature has direct inclusions bundle01 and feature02 -> bundle01's 1500 time-stamp is the max.
        assertQualifier("201205191500", projects, "feature");

    }

    public void testUnparsableIncludedArtifactQualifier() throws Exception {
        File basedir = getBasedir("projects/stablebuildqualifier/unpasablequalifier");

		List<MavenProject> projects = getSortedProjects(basedir);
        MavenSession session = newMavenSession(projects.get(0), projects);

        executeMojo(session, getProject(projects, "bundle"));
        executeMojo(session, getProject(projects, "feature"));

        assertQualifier("201205191300", projects, "feature");
    }

    public void testAggregateAttachedFeatureQualifier() throws Exception {
        File basedir = getBasedir("projects/stablebuildqualifier/attachedfeature");

        List<MavenProject> projects = getSortedProjects(basedir, new File(basedir, "targetplatform"));
        MavenProject project = getProject(projects, "attachedfeature");
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);

        DefaultDependencyArtifacts dependencyArtifacts = (DefaultDependencyArtifacts) TychoProjectUtils
                .getDependencyArtifacts(reactorProject);

        // replace target platform dependencies with fake attached feature and bundle atrifacts
        ArtifactDescriptor attachedFeature = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_FEATURE,
                "attachedfeature.attached.feature", null);
        dependencyArtifacts.removeAll(attachedFeature.getKey().getType(), attachedFeature.getKey().getId());
        dependencyArtifacts.addReactorArtifact(attachedFeature.getKey(), reactorProject, "attached-feature",
                attachedFeature.getInstallableUnits());
        ArtifactDescriptor attachedBundle = dependencyArtifacts.getArtifact(ArtifactType.TYPE_ECLIPSE_PLUGIN,
                "attachedfeature.attached.bundle", null);
        dependencyArtifacts.removeAll(attachedBundle.getKey().getType(), attachedBundle.getKey().getId());
        dependencyArtifacts.addReactorArtifact(attachedBundle.getKey(), reactorProject, "attached-bundle",
                attachedBundle.getInstallableUnits());

        MavenSession session = newMavenSession(projects.get(0), projects);

        executeMojo(session, project, "build-qualifier-aggregator");

        assertQualifier("201206180600", projects, "attachedfeature");
    }

    public void testWithInvalidQualifierFormat() throws Exception {
        MavenProject project = getProject("projects/buildqualifier", "p001/pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);
        mojo.setFormat("yyyyMMdd HHmm");
        try {
            mojo.execute();
            fail();
        } catch (MojoFailureException e) {
            assertThat(e.getMessage(), containsString("Invalid build qualifier"));
        }
    }

    public void testWithInvalidForcedQualifier() throws Exception {
        MavenProject project = getProject("projects/buildqualifier", "p001/pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);
        setVariableValueToObject(mojo, "forceContextQualifier", "invalid:Qualifier");
        try {
            mojo.execute();
            fail();
        } catch (MojoFailureException e) {
            assertThat(e.getMessage(), containsString("Invalid build qualifier"));
        }
    }

    public void testInvalidQualifierDisplaysInErrorMessage() throws Exception {
        MavenProject project = getProject("projects/buildqualifier", "p001/pom.xml");
        BuildQualifierMojo mojo = createMojoWithProject(project);
        mojo.setFormat("'This qualifier should be in error message'");
        try {
            mojo.execute();
            fail();
        } catch (MojoFailureException e) {
            assertThat(e.getMessage(), containsString("This qualifier should be in error message"));
        }
    }

    private BuildQualifierMojo createMojoWithProject(MavenProject project) throws IOException, Exception {
        ArrayList<MavenProject> projects = new ArrayList<>();
        projects.add(project);
        MavenSession session = newMavenSession(projects.get(0), projects);
        BuildQualifierMojo mojo = getMojo(project, session);
        return mojo;
    }

    private void assertQualifier(String expected, List<MavenProject> projects, String artifactId) {
        MavenProject project = getProject(projects, artifactId);
        assertEquals(expected, project.getProperties().getProperty(BUILD_QUALIFIER_PROPERTY));
    }

    private void executeMojo(MavenSession session, MavenProject project) throws Exception {
        executeMojo(session, project, "build-qualifier");
    }

    protected void executeMojo(MavenSession session, MavenProject project, String goal)
            throws Exception, ComponentConfigurationException, MojoExecutionException, MojoFailureException {
        session.setCurrentProject(project);
        BuildQualifierMojo mojo = (BuildQualifierMojo) lookupConfiguredMojo(session, newMojoExecution(goal));
        mojo.execute();
    }

    private String createTimeStampInTimeZone(String timeZone, Date date) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        BuildQualifierMojo mojo = new BuildQualifierMojo();
        mojo.setFormat("yyyyMMddHHmm");
        return mojo.getQualifier(date);
    }

    private MavenProject getProject(String baseDir, String pom) throws Exception {
        File basedirFile = getBasedir(baseDir);
        File pomFile = new File(basedirFile, pom);
        MavenExecutionRequest request = newMavenExecutionRequest(pomFile);
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);
        return result.getProject();
    }

    private BuildQualifierMojo getMojo(MavenProject project, MavenSession session) throws Exception {
        BuildQualifierMojo mojo = (BuildQualifierMojo) lookupMojo("build-qualifier", project.getFile());
        setVariableValueToObject(mojo, "project", project);

        setVariableValueToObject(mojo, "session", session);

        return mojo;
    }

}
