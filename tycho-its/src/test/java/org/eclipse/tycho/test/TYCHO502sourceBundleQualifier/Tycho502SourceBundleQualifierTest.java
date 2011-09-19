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
package org.eclipse.tycho.test.TYCHO502sourceBundleQualifier;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

public class Tycho502SourceBundleQualifierTest extends AbstractTychoIntegrationTest {

    @Test
    public void testReferencedQualifierInSourceBundle() throws Exception {
        Verifier verifier = getVerifier("/TYCHO502sourceBundleQualifier", false);
        File targetDir = new File(verifier.getBasedir(), "target");
        {
            verifier.getSystemProperties().setProperty("forceContextQualifier", "old");
            verifier.executeGoal("package");
            verifier.verifyErrorFreeLog();

            String bundleQualifier = getBundleQualifier(targetDir);
            Assert.assertEquals("old", bundleQualifier);
            String referencedQualifier = getQualifierReferencedBySourceBundle(targetDir);
            Assert.assertEquals("old", referencedQualifier);
        }
        // rebuild _without clean_ and test again
        {
            verifier.getSystemProperties().setProperty("forceContextQualifier", "new");
            verifier.setAutoclean(false);
            verifier.executeGoal("package");
            verifier.verifyErrorFreeLog();
            String bundleQualifier = getBundleQualifier(targetDir);
            Assert.assertEquals("new", bundleQualifier);
            String referencedQualifier = getQualifierReferencedBySourceBundle(targetDir);
            Assert.assertEquals("new", referencedQualifier);
        }
    }

    private String getQualifierReferencedBySourceBundle(File targetDir) throws IOException {
        File sourceJar = new File(targetDir, "bundle-0.0.1-SNAPSHOT-sources.jar");
        Assert.assertTrue(sourceJar.isFile());
        Pattern versionPattern = Pattern.compile(";version=\"(.*)\";roots:=\".\"");
        Matcher matcher = versionPattern.matcher(getManifestHeaderValue("Eclipse-SourceBundle", sourceJar).trim());
        Assert.assertTrue(matcher.find());
        String referencedQualifier = matcher.group(1).split("\\.")[3];
        return referencedQualifier;
    }

    private String getBundleQualifier(File targetDir) throws IOException {
        File bundleJar = new File(targetDir, "bundle-0.0.1-SNAPSHOT.jar");
        Assert.assertTrue(bundleJar.isFile());
        String bundleQualifier = getManifestHeaderValue("Bundle-Version", bundleJar).trim().split("\\.")[3];
        return bundleQualifier;
    }

    private String getManifestHeaderValue(String key, File bundleJar) throws IOException {
        JarFile jarFile = new JarFile(bundleJar);
        try {
            return jarFile.getManifest().getMainAttributes().getValue(key);
        } finally {
            jarFile.close();
        }
    }

}
