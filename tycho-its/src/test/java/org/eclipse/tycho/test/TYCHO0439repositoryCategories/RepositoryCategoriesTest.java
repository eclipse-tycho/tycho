/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO0439repositoryCategories;

import java.io.File;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class RepositoryCategoriesTest extends AbstractTychoIntegrationTest {

    @Test
    public void testDeployableFeature() throws Exception {
        Verifier v01 = getVerifier("TYCHO0439repositoryCategories");
        v01.executeGoal("install");
        v01.verifyErrorFreeLog();

        File site = new File(v01.getBasedir(), "target/site");
        Assert.assertTrue(site.isDirectory());

        File content = new File(site, "content.jar");
        Assert.assertTrue(content.isFile());

        boolean found = false;

        XMLParser parser = new XMLParser();
        Document document = null;
        ZipUnArchiver contentJar = new ZipUnArchiver(content);
        TemporaryFolder tempFolder = new TemporaryFolder();
        tempFolder.create();
        try {
            contentJar.extract("content.xml", tempFolder.getRoot());
            document = parser.parse(new XMLIOSource(new File(tempFolder.getRoot(), "content.xml")));
        } finally {
            tempFolder.delete();
        }
        Element repository = document.getRootElement();
        all_units: for (Element unit : repository.getChild("units").getChildren("unit")) {
            for (Element property : unit.getChild("properties").getChildren("property")) {
                if ("org.eclipse.equinox.p2.type.category".equals(property.getAttributeValue("name"))
                        && Boolean.parseBoolean(property.getAttributeValue("value"))) {
                    found = true;
                    break all_units;
                }
            }
        }

        Assert.assertTrue("Custom category", found);
    }

}
