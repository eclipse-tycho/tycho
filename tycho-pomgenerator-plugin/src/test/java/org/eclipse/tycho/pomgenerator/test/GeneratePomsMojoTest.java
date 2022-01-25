/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
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
package org.eclipse.tycho.pomgenerator.test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.Mojo;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;

public class GeneratePomsMojoTest extends AbstractPomMojoTest {

    static final String GOAL = "generate-poms";

    private void generate(File baseDir, Map<String, Object> params) throws Exception {
        generate(baseDir, null, params);
    }

    private void generate(File baseDir, File[] extraDirs, Map<String, Object> params) throws Exception {
        Mojo generateMojo = lookupMojo(GROUP_ID, ARTIFACT_ID, VERSION, GOAL, null);
        setVariableValueToObject(generateMojo, "baseDir", baseDir);
        if (extraDirs != null) {
            String dirs = Arrays.stream(extraDirs).map(File::getAbsolutePath).collect(Collectors.joining(","));
            setVariableValueToObject(generateMojo, "extraDirs", dirs);
        }
        setVariableValueToObject(generateMojo, "executionEnvironment", "J2SE-1.5"); // the default value
        setVariableValueToObject(generateMojo, "repoURL", "https://download.eclipse.org/releases/latest/");
        setVariableValueToObject(generateMojo, "repoID", "eclipse-latest");
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                setVariableValueToObject(generateMojo, param.getKey(), param.getValue());
            }
        }
        generateMojo.execute();
    }

    private void generate(File baseDir) throws Exception {
        generate(baseDir, null);
    }

    public void testPluginPom() throws Exception {
        File baseDir = getBasedir("projects/simple/p001");
        generate(baseDir);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("p001", model.getGroupId());
        assertEquals("p001", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("eclipse-plugin", model.getPackaging());
        // since it it a single pom (without a parent), the tycho extensions must be there
        assertEquals("org.eclipse.tycho", model.getBuild().getPlugins().get(0).getGroupId());
        assertEquals("tycho-maven-plugin", model.getBuild().getPlugins().get(0).getArtifactId());
    }

    public void testFeaturePom() throws Exception {
        File baseDir = getBasedir("projects/simple/p002");
        generate(baseDir);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("p002", model.getGroupId());
        assertEquals("p002", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("eclipse-feature", model.getPackaging());
    }

    public void testUpdateSite() throws Exception {
        File baseDir = getBasedir("projects/simple/p003");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group-p003");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.FALSE);
        generate(baseDir, params);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("group-p003", model.getGroupId());
        assertEquals("p003", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("eclipse-update-site", model.getPackaging());
    }

    public void testRepository() throws Exception {
        File baseDir = getBasedir("projects/simple/p006");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group-p006");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.FALSE);
        generate(baseDir, params);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("group-p006", model.getGroupId());
        assertEquals("p006", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("eclipse-repository", model.getPackaging());
    }

    public void testMultibase_1_2() throws Exception {
        File baseDir = getBasedir("projects/multibase");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        generate(new File(baseDir, "base1"), new File[] { new File(baseDir, "base2") }, params);

        Model parent1 = readModel(baseDir, "base1/pom.xml");
        List<String> modules1 = parent1.getModules();
        assertEquals(6, modules1.size());

        assertFalse(new File(baseDir, "base2/pom.xml").exists());

        Model aggmodel = readModel(baseDir, "base2/p006/poma.xml");
        List<String> aggrmodules = aggmodel.getModules();
        assertEquals(5, aggrmodules.size());
        assertEquals("../../base1/p002", aggrmodules.get(1));
        assertEquals("../p005", aggrmodules.get(3));
    }

    public void testRecursive() throws Exception {
        File baseDir = getBasedir("projects/multibase");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        generate(baseDir, new File[] {}, params);

        Model parent = readModel(baseDir, "pom.xml");
        assertEquals(6, parent.getModules().size());
    }

    public void testParent() throws Exception {
        File baseDir = getBasedir("projects/simple");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        generate(baseDir, params);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("group", model.getGroupId());
        assertEquals("simple", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("pom", model.getPackaging());

        List<Repository> repositories = model.getRepositories();
        assertEquals(1, repositories.size());
        Repository repo = repositories.get(0);
        assertEquals("p2", repo.getLayout());
        assertEquals("https://download.eclipse.org/releases/latest/", repo.getUrl());
        assertEquals("eclipse-latest", repo.getId());

        List<String> modules = model.getModules();
        assertEquals(6, modules.size());

        Model p002 = readModel(baseDir, "p002/pom.xml");

        assertEquals("group", p002.getParent().getGroupId());

        Model aggmodel = readModel(baseDir, "p003/poma.xml");
        assertEquals("p003.aggregator", aggmodel.getArtifactId());
        List<String> aggrmodules = aggmodel.getModules();
        assertEquals(4, aggrmodules.size());
        // pick up fragments only when they are explicitly referenced from a feature
        assertFalse(aggrmodules.contains("../p004"));
    }

    public void testTests() throws Exception {
        File baseDir = getBasedir("projects/tests");

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        params.put("testSuffix", ".tests");
        params.put("testSuite", "p004");
        generate(baseDir, params);

        Model aggmodel = readModel(baseDir, "p003/poma.xml");
        List<String> aggrmodules = aggmodel.getModules();
        assertEquals(5, aggrmodules.size());
        assertEquals(Arrays.asList("../p001", "../p001.tests", "../p002", "../p004", "."), aggrmodules);

        assertEquals("eclipse-test-plugin", readModel(baseDir, "p001.tests/pom.xml").getPackaging());
        assertEquals("eclipse-test-plugin", readModel(baseDir, "p004/pom.xml").getPackaging());
    }

    public void testRootProjects() throws Exception {
        File baseDir = getBasedir("projects/rootprojects");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        params.put("rootProjects", new File(baseDir, "p004").getAbsolutePath());
        generate(baseDir, params);

        Model parent = readModel(baseDir, "pom.xml");
        assertEquals(3, parent.getModules().size());

        Model aggmodel = readModel(baseDir, "p004/poma.xml");
        assertEquals(3, aggmodel.getModules().size()); // don't forger . module
    }

    public void testDeepModule() throws Exception {
        File baseDir = getBasedir("projects/deepmodule");

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        generate(baseDir, new File[] { new File(baseDir, "base") }, params);

        Model module = readModel(baseDir, "base/p001/pom.xml");

        assertEquals("../../", module.getParent().getRelativePath());

    }

    public void testMalformedManifest() throws Exception {
        File baseDir = getBasedir("projects/malformed");
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        try {
            generate(baseDir, new File[] { new File(baseDir, "base") }, params);
            fail("OsgiManifestParserException expected, since  it does contain an invalid MANIFEST.MF file");
        } catch (OsgiManifestParserException e) {
            assertFalse(new File(baseDir, "pom.xml").exists());
        }

    }

    public void testWithMetadataDirectory() throws Exception {
        File baseDir = getBasedir("projects/withmetadata");
        generate(baseDir);
        Model model = readModel(baseDir, "pom.xml");
        assertNotNull(model);
        assertTrue("Only one module must be present, .metadata must be skipped", model.getModules().size() == 1);
    }
}
