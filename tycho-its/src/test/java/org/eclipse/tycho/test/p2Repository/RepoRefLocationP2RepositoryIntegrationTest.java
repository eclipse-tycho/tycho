/*******************************************************************************
 * Copyright (c) 2019 Guillaume Dufour and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Guillaume Dufour - support repo ref location (453708)
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.P2RepositoryTool;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class RepoRefLocationP2RepositoryIntegrationTest extends AbstractTychoIntegrationTest {

    private static Verifier verifier;
    private static P2RepositoryTool p2Repo;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void executeBuild() throws Exception {
        verifier = new RepoRefLocationP2RepositoryIntegrationTest().getVerifier("/p2Repository.repositoryRef.location",
                false);
        verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_OXYGEN.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
    }

    @Test
    public void testRefLocation() throws Exception {
        File repository = new File(verifier.getBasedir(), "target/repository");
        File contentXml = new File(repository, "content.xml");
        assertThat(contentXml, isFile());
        File artifactXml = new File(repository, "artifacts.xml");
        assertThat(artifactXml, isFile());
        assertThat(new File(repository, "category.xml"), isFile());

        Map<String, String> expected = new HashMap<>(2, 1.f);
        expected.put("http://some.where", "false");
        expected.put("http://some.where.else", "true");
        Document artifactsDocument = XMLParser.parse(contentXml);
        // See MetadataRepositoryIO.Writer#writeRepositoryReferences
        artifactsDocument.getChild("repository").getChild("references").getChildren("repository").forEach(element -> {
            String location = element.getAttributeValue("uri");
            if (expected.containsKey(location) && expected.get(location).equals(element.getAttributeValue("enabled"))) {
                expected.remove(location);
            } else {
                fail("Unexpected repository reference in artifact repository " + element);
            }
        });
        assertEquals("Missing repository reference in artifact repository", Collections.emptyMap(), expected);

    }

}
