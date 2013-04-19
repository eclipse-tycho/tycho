/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.io.File;
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
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;
import org.junit.Ignore;

@Ignore("maven-plugin-testing harness broken with maven 3.1-SNAPSHOT")
public class BuildQualifierTest extends AbstractTychoMojoTestCase {

    public void testForceContextQualifier() throws Exception {
        /*
         * This test covers all scenarios that involve forceContextQualifier mojo parameter, i.e.
         * setting -DforceContextQualifier=... on cli, specifying forceContextQualifier project
         * property and setting forceContextQualifier using explicit mojo configuration.
         */

        File basedir = getBasedir("projects/buildqualifier");

        File pom = new File(basedir, "p001/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);

        MavenProject project = getProject(request);
        project.getProperties().put(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage");

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo(project, session);

        setVariableValueToObject(mojo, "forceContextQualifier", "foo-bar");

        mojo.execute();

        assertEquals("foo-bar", project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
    }

    public void testBuildProperties() throws Exception {
        File basedir = getBasedir("projects/buildqualifier");

        File pom = new File(basedir, "p002/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);

        MavenProject project = getProject(request);
        project.getProperties().put(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage");

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo(project, session);

        mojo.execute();

        assertEquals("blah", project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
    }

    public void testTimestamp() throws Exception {
        File basedir = getBasedir("projects/buildqualifier");

        File pom = new File(basedir, "p001/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);

        MavenProject project = getProject(request);
        project.getProperties().put(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY, "garbage");

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);

        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo(project, session);

        mojo.execute();

        String firstTimestamp = (String) project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY);

        // lets do it again
        Thread.sleep(500L);

        project = getProject(request);
        assertNull(project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
        mojo = getMojo(project, session);
        mojo.execute();

        assertEquals(firstTimestamp, project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
    }

    public void testUnqualifiedVersion() throws Exception {
        File basedir = getBasedir("projects/buildqualifier");
        File pom = new File(basedir, "p002/pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);

        MavenProject project = getProject(request);

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);
        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo(project, session);

        mojo.execute();

        assertEquals("0.0.1", project.getProperties().get(BuildQualifierMojo.UNQUALIFIED_VERSION_PROPERTY));
    }

    public void testFullyQualifiedVersion() throws Exception {
        File basedir = getBasedir("projects/buildqualifier/fullyqualified");
        File pom = new File(basedir, "pom.xml");

        MavenExecutionRequest request = newMavenExecutionRequest(pom);
        request.getProjectBuildingRequest().setProcessPlugins(false);

        MavenProject project = getProject(request);

        ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
        projects.add(project);
        MavenSession session = new MavenSession(getContainer(), request, null, projects);

        BuildQualifierMojo mojo = getMojo(project, session);

        mojo.execute();

        assertEquals("0.0.1", project.getProperties().get(BuildQualifierMojo.UNQUALIFIED_VERSION_PROPERTY));
        assertEquals("123456", project.getProperties().get(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
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

        List<MavenProject> projects = getSortedProjects(basedir, null);
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

        List<MavenProject> projects = getSortedProjects(basedir, null);
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

        TychoProject projectType = lookup(TychoProject.class, ArtifactKey.TYPE_ECLIPSE_FEATURE);
        DefaultTargetPlatform dependencyArtifacts = (DefaultTargetPlatform) projectType.getDependencyArtifacts(project);

        // replace target platform dependencies with fake attached feature and bundle atrifacts
        ArtifactDescriptor attachedFeature = dependencyArtifacts.getArtifact(ArtifactKey.TYPE_ECLIPSE_FEATURE,
                "attachedfeature.attached.feature", null);
        dependencyArtifacts.removeAll(attachedFeature.getKey().getType(), attachedFeature.getKey().getId());
        dependencyArtifacts.addReactorArtifact(attachedFeature.getKey(), reactorProject, "attached-feature",
                attachedFeature.getInstallableUnits());
        ArtifactDescriptor attachedBundle = dependencyArtifacts.getArtifact(ArtifactKey.TYPE_ECLIPSE_PLUGIN,
                "attachedfeature.attached.bundle", null);
        dependencyArtifacts.removeAll(attachedBundle.getKey().getType(), attachedBundle.getKey().getId());
        dependencyArtifacts.addReactorArtifact(attachedBundle.getKey(), reactorProject, "attached-bundle",
                attachedBundle.getInstallableUnits());

        MavenSession session = newMavenSession(projects.get(0), projects);

        executeMojo(session, project, "build-qualifier-aggregator");

        assertQualifier("201206180600", projects, "attachedfeature");
    }

    private void assertQualifier(String expected, List<MavenProject> projects, String artifactId) {
        MavenProject project = getProject(projects, artifactId);
        assertEquals(expected, project.getProperties().getProperty(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
    }

    private void executeMojo(MavenSession session, MavenProject project) throws Exception {
        executeMojo(session, project, "build-qualifier");
    }

    protected void executeMojo(MavenSession session, MavenProject project, String goal) throws Exception,
            ComponentConfigurationException, MojoExecutionException, MojoFailureException {
        session.setCurrentProject(project);
        BuildQualifierMojo mojo = (BuildQualifierMojo) lookupConfiguredMojo(session, newMojoExecution(goal));
        mojo.execute();
        ReactorProject reactorProject = DefaultReactorProject.adapt(project);
        String version = lookup(TychoProject.class, project.getPackaging()).getArtifactKey(reactorProject).getVersion();
        reactorProject.setExpandedVersion(version,
                project.getProperties().getProperty(BuildQualifierMojo.BUILD_QUALIFIER_PROPERTY));
    }

    private String createTimeStampInTimeZone(String timeZone, Date date) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        BuildQualifierMojo mojo = new BuildQualifierMojo();
        mojo.setFormat("yyyyMMddHHmm");
        return mojo.getQualifier(date);
    }

    private MavenProject getProject(MavenExecutionRequest request) throws Exception {
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
