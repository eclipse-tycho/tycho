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
package org.eclipse.tycho.buildversion;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

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
