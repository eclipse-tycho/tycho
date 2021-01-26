/*******************************************************************************
 * Copyright (c) 2019, 2021 Guillaume Dufour and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Guillaume Dufour - support repo ref location (453708)
 *******************************************************************************/
package org.eclipse.tycho.test.p2Repository;

import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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

    @BeforeClass
    public static void executeBuild() throws Exception {
        verifier = new RepoRefLocationP2RepositoryIntegrationTest().getVerifier("/p2Repository.repositoryRef.location",
                false);
        verifier.getCliOptions().add("-Dtest-data-repo=" + ResourceUtil.P2Repositories.ECLIPSE_LATEST.toString());
        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
        p2Repo = P2RepositoryTool.forEclipseRepositoryModule(new File(verifier.getBasedir()));
    }

    @Test
    public void testRefLocation() throws Exception {
        File target = new File(verifier.getBasedir(), "target");
        File repository = new File(target, "repository");
        File contentXml = new File(repository, "content.xml");
        assertThat(contentXml, isFile());
        File artifactXml = new File(repository, "artifacts.xml");
        assertThat(artifactXml, isFile());
        assertThat(new File(target, "category.xml"), isFile());

        Map<String, Boolean> expected = new HashMap<>(2, 1.f);
        expected.put("http://some.where", false);
        expected.put("http://some.where.else", true);
        Document artifactsDocument = XMLParser.parse(contentXml);
        // See MetadataRepositoryIO.Writer#writeRepositoryReferences
        artifactsDocument.getChild("repository").getChild("references").getChildren("repository").forEach(element -> {
            String location = element.getAttributeValue("uri");
            if (expected.containsKey(location)
                    && expected.get(location).equals(element.getAttributeValue("options").equals("1"))) {
                expected.remove(location);
            } else {
                System.out.println(location);
                System.out.println(expected.containsKey(location));
                System.out.println(expected.get(location));
                System.out.println();
                fail("Unexpected repository reference in artifact repository " + element);
            }
        });
        assertEquals("Missing repository reference in artifact repository", Collections.emptyMap(), expected);

    }

}
