/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.TYCHO502sourceBundleQualifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho502SourceBundleQualifierTest extends AbstractTychoIntegrationTest {

	@Test
	public void testReferencedQualifierInSourceBundle() throws Exception {
		Verifier verifier = getVerifier("/TYCHO502sourceBundleQualifier", false);
		File targetDir = new File(verifier.getBasedir(), "target");
		{
			verifier.addCliArgument("-DforceContextQualifier=old");
			verifier.executeGoal("package");
			verifier.verifyErrorFreeLog();

			String bundleQualifier = getBundleQualifier(targetDir);
			assertEquals("old", bundleQualifier);
			String referencedQualifier = getQualifierReferencedBySourceBundle(targetDir);
			assertEquals("old", referencedQualifier);
		}
		// rebuild _without clean_ and test again
		{
			verifier.addCliArgument("-DforceContextQualifier=new");
			verifier.setAutoclean(false);
			verifier.executeGoal("package");
			verifier.verifyErrorFreeLog();
			String bundleQualifier = getBundleQualifier(targetDir);
			assertEquals("new", bundleQualifier);
			String referencedQualifier = getQualifierReferencedBySourceBundle(targetDir);
			assertEquals("new", referencedQualifier);
		}
	}

	private String getQualifierReferencedBySourceBundle(File targetDir) throws IOException {
		File sourceJar = new File(targetDir, "bundle-0.0.1-SNAPSHOT-sources.jar");
		assertTrue(sourceJar.isFile());
		Pattern versionPattern = Pattern.compile(";version=\"(.*)\";roots:=\".\"");
		Matcher matcher = versionPattern.matcher(getManifestHeaderValue("Eclipse-SourceBundle", sourceJar).trim());
		assertTrue(matcher.find());
		return matcher.group(1).split("\\.")[3];
	}

	private String getBundleQualifier(File targetDir) throws IOException {
		File bundleJar = new File(targetDir, "bundle-0.0.1-SNAPSHOT.jar");
		assertTrue(bundleJar.isFile());
		return getManifestHeaderValue("Bundle-Version", bundleJar).trim().split("\\.")[3];
	}

	private String getManifestHeaderValue(String key, File bundleJar) throws IOException {
		try (JarFile jarFile = new JarFile(bundleJar)) {
			return jarFile.getManifest().getMainAttributes().getValue(key);
		}
	}

}
