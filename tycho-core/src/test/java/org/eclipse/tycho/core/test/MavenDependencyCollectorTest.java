/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.testing.AbstractLegacyTychoMojoTestBase;
import org.eclipse.tycho.testing.CompoundRuntimeException;
import org.junit.Assert;

public class MavenDependencyCollectorTest extends AbstractLegacyTychoMojoTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testNestedJars() throws Exception {
        File targetPlatform = getBasedir("targetplatforms/nestedJar");
        List<MavenProject> projects = getSortedProjects(getBasedir("projects/mavendeps"), targetPlatform);
        {
            // 1. project with dependency to external bundle with nested jar
            MavenProject project = projects.get(1);
            final String plainJarPath = "target/targetplatforms/nestedJar/plugins/nested_1.0.0.jar";
            final String nestedJarPath = "target/local-repo/.cache/tycho/nested_1.0.0.jar/lib/lib.jar";
            List<Dependency> mavenDependencies = project.getModel().getDependencies();
            Assert.assertEquals(2, mavenDependencies.size());
            final String expectedGroupId = "p2.eclipse-plugin";
            final String expectedArtifactId = "nested";
            final String expectedVersion = "1.0.0";
            final String expectedType = "jar";
            final String expectedScope = Artifact.SCOPE_SYSTEM;
            // assert that dependencies to both plain jar and nested jar are injected back into maven model
            assertDependenciesContains(mavenDependencies, expectedGroupId, expectedArtifactId, expectedVersion, null,
                    expectedType, expectedScope, new File(getBasedir(), plainJarPath));
            assertDependenciesContains(mavenDependencies, expectedGroupId, expectedArtifactId, expectedVersion,
                    "lib/lib.jar", expectedType, expectedScope, new File(getBasedir(), nestedJarPath));
        }
        {
            // 2. project with checked-in nested jar
            MavenProject project = projects.get(2);
            List<Dependency> mavenDependencies = project.getModel().getDependencies();
            assertEquals(1, mavenDependencies.size());
            assertDependenciesContains(mavenDependencies, "mavenDependencies", "p002", "1.0.0", "lib/lib.jar", "jar",
                    Artifact.SCOPE_SYSTEM, new File(getBasedir("projects/mavendeps"), "p002/lib/lib.jar"));
        }
        {
            // 3. project with dependency to bundle with nested jar within the same reactor
            MavenProject project = projects.get(3);
            List<Dependency> mavenDependencies = project.getModel().getDependencies();
            // assert that dependencies to both reactor module and checked-in nested jar are injected back into maven
            // model.
            // Also, dependency to missing nested jar must *not* be injected (would throw
            // MavenDependencyResolutionException otherwise)
            Assert.assertEquals(2, mavenDependencies.size());
            final String expectedGroupId = "mavenDependencies";
            final String expectedArtifactId = "p002";
            final String expectedVersion = "1.0.0";
            assertDependenciesContains(mavenDependencies, expectedGroupId, expectedArtifactId, expectedVersion, null,
                    "eclipse-plugin", Artifact.SCOPE_PROVIDED, null);
            assertDependenciesContains(mavenDependencies, expectedGroupId, expectedArtifactId, expectedVersion,
                    "lib/lib.jar", "jar", Artifact.SCOPE_SYSTEM, new File(getBasedir("projects/mavendeps"),
                            "p002/lib/lib.jar"));
        }
    }

    public void testModuleOrderNoDotOnClasspath() throws Exception {
        File pom = new File(getBasedir("projects/nodotonclasspath"), "pom.xml");
        List<MavenProject> projects = getSortedProjects(newMavenExecutionRequest(pom));
        assertEquals(3, projects.size());
        MavenProject project1 = (MavenProject) projects.get(1);
        MavenProject project2 = (MavenProject) projects.get(2);
        Assert.assertEquals("provider", project1.getArtifactId());
        Assert.assertEquals("consumer", project2.getArtifactId());
    }

    public void testInjectDuplicateSourceFolders() throws Exception {
        File pom = new File(getBasedir("projects/sourceFolders"), "pom.xml");
        List<MavenProject> projects = getSortedProjects(newMavenExecutionRequest(pom));
        MavenProject project = (MavenProject) projects.get(0);
        List<File> sourceRootFiles = new ArrayList<File>();
        for (String compileRoot : project.getCompileSourceRoots()) {
            sourceRootFiles.add(new File(compileRoot));
        }
        List<File> testRootFiles = new ArrayList<File>();
        for (String testCompileRoot : project.getTestCompileSourceRoots()) {
            testRootFiles.add(new File(testCompileRoot));
        }
        assertTrue(Collections.disjoint(sourceRootFiles, testRootFiles));
    }

    private void assertDependenciesContains(List<Dependency> mavenDependencies, String groupId, String artifactId,
            String version, String classifier, String type, String scope, File systemLocation) throws IOException {
        for (Dependency dependency : mavenDependencies) {
            boolean systemLocationEquals = true;
            if (systemLocation != null) {
                systemLocationEquals = dependency.getSystemPath() != null
                        && systemLocation.getCanonicalFile().getAbsolutePath()
                                .equals(new File(dependency.getSystemPath()).getCanonicalFile().getAbsolutePath());
            }
            if (systemLocationEquals //
                    && groupId.equals(dependency.getGroupId()) //
                    && artifactId.equals(dependency.getArtifactId())//
                    && version.equals(dependency.getVersion()) //
                    && type.equals(dependency.getType())//
                    && scope.equals(dependency.getScope()) //
            ) {
                if (classifier == null) {
                    if (dependency.getClassifier() == null) {
                        return;
                    }
                } else {
                    if (classifier.equals(dependency.getClassifier())) {
                        return;
                    }
                }
            }
        }
        fail("Expected dependency [" + groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type + ":"
                + scope + ":" + systemLocation + "] not found in actual dependencies: "
                + toDebugString(mavenDependencies));
    }

    private static String toDebugString(List<Dependency> mavenDependencies) {
        StringBuilder sb = new StringBuilder();
        for (Dependency dependency : mavenDependencies) {
            sb.append(toDebugString(dependency));
        }
        return sb.toString();
    }

    private static String toDebugString(Dependency dependency) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(dependency.getGroupId());
        sb.append(':');
        sb.append(dependency.getArtifactId());
        sb.append(':');
        sb.append(dependency.getVersion());
        sb.append(':');
        sb.append(dependency.getClassifier());
        sb.append(':');
        sb.append(dependency.getType());
        sb.append(", scope: ");
        sb.append(dependency.getScope());
        sb.append(", systemPath: ");
        sb.append(dependency.getSystemPath());
        sb.append(']');
        return sb.toString();
    }

    private List<MavenProject> getSortedProjects(MavenExecutionRequest request) {
        request.getProjectBuildingRequest().setProcessPlugins(false);
        MavenExecutionResult result = maven.execute(request);
        if (result.hasExceptions()) {
            throw new CompoundRuntimeException(result.getExceptions());
        }
        return result.getTopologicallySortedProjects();
    }

}
