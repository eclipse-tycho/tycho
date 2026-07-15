/*******************************************************************************
 * Copyright (c) 2012, 2021 SAP AG and others.
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

package org.eclipse.tycho.test.packaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class PackageNestedJarsAndDirsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testPackageNestedJarsAndDirs() throws Exception {
		Verifier verifier = getVerifier("/packaging.nestedJarsAndDirs", false);
		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
		File bundleJar = new File(verifier.getBasedir(), "target/nestedJarsAndDirs-1.0.0-SNAPSHOT.jar");
		assertTrue(bundleJar.isFile());
		try (JarFile jarFile = new JarFile(bundleJar)) {
			// included via additional filesets
			assertFileEntryExists("foo.txt", jarFile);
			assertNull(jarFile.getEntry("bar.txt"));
			// included via bin.includes
			assertFileEntryExists("resources/test.txt", jarFile);
			assertFileEntryExists("org/eclipse/tycho/its/nestedJarsAndDirs/Main.class", jarFile);
			assertFileEntryExists("internal2/org/eclipse/tycho/its/nestedJarsAndDirs/internal2/Internal2.class",
					jarFile);
			String internal1Jar = "internal1.jar";
			ZipEntry nestedJarEntry = assertFileEntryExists(internal1Jar, jarFile);

			try (InputStream stream = jarFile.getInputStream(nestedJarEntry);
					ZipInputStream zis = new ZipInputStream(stream)) {
				ZipEntry nestedEntry = null;
				boolean found = false;
				String internal1ClassName = "org/eclipse/tycho/its/nestedJarsAndDirs/internal1/Internal1.class";
				while ((nestedEntry = zis.getNextEntry()) != null) {
					if (internal1ClassName.equals(nestedEntry.getName())) {
						found = true;
						break;
					}
				}
				assertTrue(internal1ClassName + " not found in nested jar " + internal1Jar, found);
			}
		}
	}

	private ZipEntry assertFileEntryExists(String entry, JarFile jarFile) {
		ZipEntry jarEntry = jarFile.getEntry(entry);
		assertNotNull("entry '" + entry + " does not exist in " + jarFile.getName(), jarEntry);
		assertFalse("entry '" + entry + " exists in " + jarFile.getName() + " but is a directory",
				jarEntry.isDirectory());
		return jarEntry;
	}

}
