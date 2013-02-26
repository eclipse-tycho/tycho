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
package org.eclipse.tycho.pomgenerator.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class GeneratePomsMojoTest extends AbstractTychoMojoTestCase {

    MavenXpp3Reader modelReader = new MavenXpp3Reader();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private void generate(File baseDir, Map<String, Object> params) throws Exception {
        generate(baseDir, null, params);
    }

    private void generate(File baseDir, File[] extraDirs, Map<String, Object> params) throws Exception {
        Mojo generateMojo = lookupMojo("org.eclipse.tycho", "tycho-pomgenerator-plugin",
                TychoVersion.getTychoVersion(), "generate-poms", null);
        setVariableValueToObject(generateMojo, "baseDir", baseDir);
        if (extraDirs != null) {
            StringBuilder sb = new StringBuilder();
            for (File dir : extraDirs) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(dir.getAbsolutePath());
            }
            setVariableValueToObject(generateMojo, "extraDirs", sb.toString());
        }
        setVariableValueToObject(generateMojo, "executionEnvironment", "J2SE-1.5"); // the default value
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
        Map<String, Object> params = new HashMap<String, Object>();
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

    public void testMultibase_1_2() throws Exception {
        File baseDir = getBasedir("projects/multibase");
        Map<String, Object> params = new HashMap<String, Object>();
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

    public void testParent() throws Exception {
        File baseDir = getBasedir("projects/simple");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        generate(baseDir, params);
        Model model = readModel(baseDir, "pom.xml");

        assertEquals("group", model.getGroupId());
        assertEquals("simple", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("pom", model.getPackaging());

        List modules = model.getModules();
        assertEquals(5, modules.size());

        Model p002 = readModel(baseDir, "p002/pom.xml");

        assertEquals("group", p002.getParent().getGroupId());

        Model aggmodel = readModel(baseDir, "p003/poma.xml");
        assertEquals("p003.aggregator", aggmodel.getArtifactId());
        List<String> aggrmodules = aggmodel.getModules();
        assertEquals(4, aggrmodules.size());
        // pick up fragments only when they are explicitly referenced from a feature
        assertFalse(aggrmodules.contains("../p004"));
    }

    private Model readModel(File baseDir, String name) throws IOException, XmlPullParserException {
        File pom = new File(baseDir, name);
        FileInputStream is = new FileInputStream(pom);
        try {
            return modelReader.read(ReaderFactory.newXmlReader(is));
        } finally {
            is.close();
        }
    }

    public void testTests() throws Exception {
        File baseDir = getBasedir("projects/tests");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        params.put("aggregator", Boolean.TRUE);
        params.put("testSuffix", ".tests");
        params.put("testSuite", "p004");
        generate(baseDir, params);

        Model aggmodel = readModel(baseDir, "p003/poma.xml");
        List<String> aggrmodules = aggmodel.getModules();
        assertEquals(5, aggrmodules.size());
        assertEquals(Arrays.asList(new String[] { "../p001", "../p001.tests", "../p002", "../p004", "." }), aggrmodules);

        assertEquals("eclipse-test-plugin", readModel(baseDir, "p001.tests/pom.xml").getPackaging());
        assertEquals("eclipse-test-plugin", readModel(baseDir, "p004/pom.xml").getPackaging());
    }

    public void testRootProjects() throws Exception {
        File baseDir = getBasedir("projects/rootprojects");
        Map<String, Object> params = new HashMap<String, Object>();
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

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", "group");
        params.put("version", "1.0.0");
        generate(baseDir, new File[] { new File(baseDir, "base") }, params);

        Model module = readModel(baseDir, "base/p001/pom.xml");

        assertEquals("../../", module.getParent().getRelativePath());

    }
}
