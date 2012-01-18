/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.bug368985_licenseFeature;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Assert;
import org.junit.Test;

public class LicenseFeatureTest extends AbstractTychoIntegrationTest {
    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("/368985_licenseFeature", false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();

        assertFeatureJar(new File(verifier.getBasedir(),
                "repository/target/repository/features/feature_1.2.3.123abc.jar"));
        assertFeatureJar(new File(verifier.getBasedir(),
                "repository/target/repository/features/feature.conflicting-dependencies_1.2.3.123abc.jar"));
    }

    protected void assertFeatureJar(File feature) throws ZipException, IOException {
        assertTrue(feature.canRead());

        ZipFile zip = new ZipFile(feature);

        try {

            Assert.assertNotNull(zip.getEntry("file1.txt"));
            Assert.assertNotNull(zip.getEntry("file2.txt"));

            Properties p = new Properties();
            InputStream is = zip.getInputStream(zip.getEntry("feature.properties"));
            try {
                p.load(is);
            } finally {
                IOUtil.close(is);
            }

            Assert.assertEquals("file1.txt", p.getProperty("licenseURL"));
            Assert.assertEquals("License - The More The Merrier.", p.getProperty("license"));
        } finally {
            zip.close();
        }
    }

}
