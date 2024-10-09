/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
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
package org.eclipse.tycho.test.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Assert;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParser;

public class Tycho465RootFilesTest extends AbstractTychoIntegrationTest {

	static final String QUALIFIER = "forced";
	static final String MODULE = "eclipse-repository";

	@Test
	public void testProductBuild() throws Exception {
		Verifier verifier = getVerifier("product.rootFiles", true);

		verifier.addCliOption("-DforceContextQualifier=" + QUALIFIER.toString());

		verifier.executeGoal("install");
		verifier.verifyErrorFreeLog();

		File targetDir = new File(verifier.getBasedir(), MODULE + "/target");
		File repositoryTargetDirectory = new File(targetDir, "repository");

		Document contentXml = openMetadataRepositoryDocument(repositoryTargetDirectory);

		assertBuildProductAndRepository(targetDir, repositoryTargetDirectory, contentXml);

		// clean the local build results
		verifier.executeGoal("clean");
		verifier.verifyErrorFreeLog();

		// re-build the repository project only (incl. products) to ensure that the
		// created root file zips were attached
		// to the project and are available from the local repository
		final boolean ignoreLocallyInstalledArtifacts = false;
		Verifier eclipseRepoProjectVerifier = getVerifier("product.rootFiles/eclipse-repository", true,
				ignoreLocallyInstalledArtifacts);

		eclipseRepoProjectVerifier.addCliOption("-DforceContextQualifier=" + QUALIFIER.toString());

		eclipseRepoProjectVerifier.executeGoal("verify");
		eclipseRepoProjectVerifier.verifyErrorFreeLog();

		Document updatedContentXml = openMetadataRepositoryDocument(repositoryTargetDirectory);

		assertBuildProductAndRepository(targetDir, repositoryTargetDirectory, updatedContentXml);
	}

	private void assertBuildProductAndRepository(File targetDir, File repositoryTargetDirectory, Document contentXml) {
		assertCategoryIU(contentXml, QUALIFIER + ".category.id", "prf.feature.feature.group");
		assertFeatureIU(contentXml, repositoryTargetDirectory, "prf.feature");

		assertRootIuMetaData(contentXml);
		assertInstalledWinConfigRootFile(targetDir);
		assertInstalledLinuxConfigRootFile(targetDir);

		assertRootIuPermissionsMetaData(contentXml);
		assertRootIuLinksMetaData(contentXml);
	}

	static String getFileNotExistsInDirMsg(String fileRootRelPath, File dir) {
		return ("Expected root file '" + fileRootRelPath + "' does not exist in directory " + dir.toURI());
	}

	static void assertContainsEntry(File dir, String prefix) {
		File[] listFiles = dir.listFiles();

		for (File file : listFiles) {
			if (file.getName().startsWith(prefix)) {
				if (file.getName().endsWith(".qualifier.jar")) {
					Assert.fail("replacement of build qualifier missing in file " + file + ", name: " + file.getName());
				}
				return;
			}
		}
		Assert.fail("Missing entry " + prefix + "* in assembled repository directory " + dir);
	}

	static void assertFeatureIU(Document contentXml, File assembledRepoDir, String featureId, String... requiredIus) {
		String featureIuId = featureId + ".feature.group";
		Set<Element> featureIus = findIU(contentXml, featureIuId);
		assertEquals("Feature iu with id = '" + featureIuId + "' does not occur exactly once in content.xml", 1,
				featureIus.size());

		Element featureIu = featureIus.iterator().next();

		assertTrue(containsIUWithProperty(contentXml, featureIuId, "org.eclipse.equinox.p2.type.group", "true"));
		assertTrue(iuHasAllRequirements(featureIu, requiredIus));

		String featureArtifactPrefix = featureId + "_1.0.0";
		assertContainsEntry(new File(assembledRepoDir, "features/"), featureArtifactPrefix);
	}

	static void assertCategoryIU(Document contentXml, String categoryIuId, String featureIuId) {
		Set<Element> categoryIus = findIU(contentXml, categoryIuId);
		assertEquals("Unique category iu not found", 1, categoryIus.size());
		Element categoryIu = categoryIus.iterator().next();

		assertTrue("IU not typed as category",
				iuHasProperty(categoryIu, "org.eclipse.equinox.p2.type.category", "true"));
		assertTrue("Category name missing", iuHasProperty(categoryIu, "org.eclipse.equinox.p2.name", "A Category"));
		assertTrue(iuHasAllRequirements(categoryIu, featureIuId));
	}

	static void assertAddedRootFile(File targetDir) {
		String relRootFilePath = "addedFile.txt";

		File mainWinConfigProductDir = new File(targetDir, "products/main.product.id/win32/win32/x86_64");
		File rootFile = new File(mainWinConfigProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		File mainLinuxConfigProductDir = new File(targetDir, "products/main.product.id/linux/gtk/x86_64");
		rootFile = new File(mainLinuxConfigProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());
	}

	static void assertConfigIndependentRootFiles(File mainProductDir) {
		String relRootFilePath = "rootFile.txt";
		File rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "file5.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "dir/file6.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());
	}

	static void assertInstalledLinuxConfigRootFile(File targetDir) {
		File mainProductDir = new File(targetDir, "products/main.product.id/linux/gtk/x86_64");
		String relRootFilePath = "file1.txt";
		File rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "dir/file2.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		// without config specified root files => included all config specific products
		assertConfigIndependentRootFiles(mainProductDir);
	}

	static void assertInstalledWinConfigRootFile(File targetDir) {
		File mainProductDir = new File(targetDir, "products/main.product.id/win32/win32/x86_64");
		String relRootFilePath = "file1.txt";
		File rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "file2.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "dir1/file3.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		relRootFilePath = "dir1/dir2/file4.txt";
		rootFile = new File(mainProductDir, relRootFilePath);

		assertTrue(getFileNotExistsInDirMsg(relRootFilePath, rootFile), rootFile.exists());

		// without config specified root files => included all config specific products
		assertConfigIndependentRootFiles(mainProductDir);
	}

	static void assertRootIuMetaData(Document contentXml) {
		String featureId = "prf.feature";
		String featureIuId = featureId + ".feature.group";
		Set<Element> featureIus = findIU(contentXml, featureIuId);

		assertEquals("Feature iu with id = '" + featureIuId + "' does not occur exactly once in content.xml", 1,
				featureIus.size());

		Element featureIu = featureIus.iterator().next();
		String rootWinConfigFeatureIuId = featureId + "_root.win32.win32.x86_64";

		assertTrue(
				"Verifying content.xml failed because feature iu with id = '" + featureIuId
						+ "' does not contain required root iu with id = '" + rootWinConfigFeatureIuId + "'",
				iuHasAllRequirements(featureIu, rootWinConfigFeatureIuId));

		String rootLinuxConfigFeatureIuId = featureId + "_root.gtk.linux.x86_64";

		assertTrue(
				"Verifying content.xml failed because feature iu with id = '" + featureIuId
						+ "' does not contain required root iu with id = '" + rootLinuxConfigFeatureIuId + "'",
				iuHasAllRequirements(featureIu, rootLinuxConfigFeatureIuId));

		String featureIuRootId = featureId + "_root";
		Set<Element> featureRootIus = findIU(contentXml, featureIuRootId);

		assertEquals("Feature root iu with id = '" + featureIuRootId + "' does not occur exactly once in content.xml",
				1, featureRootIus.size());
	}

	static void assertRootIuPermissionsMetaData(Document contentXml) {
		// permission defined in build.properties: root.permissions.755 = file5.txt
		Set<Element> featureRootIus = findIU(contentXml, "prf.feature_root");

		String expectedTouchpointDataInstruction = "chmod(targetDir:${installFolder}, targetFile:file5.txt, permissions:755);";

		assertTrue("Expected chmod touchpointData instruction '" + expectedTouchpointDataInstruction + "' not found.",
				iuHasTouchpointDataInstruction(featureRootIus.iterator().next(), expectedTouchpointDataInstruction));
		// permission defined in build.properties: root.linux.gtk.x86_64.permissions.555
		// = **/*.so
		Element linuxRootIu = findIU(contentXml, "prf.feature_root.gtk.linux.x86_64").iterator().next();

		String chmod555Instruction = "chmod(targetDir:${installFolder}, targetFile:dir/test.so, permissions:555);";

		assertTrue("Expected chmod touchpointData instruction '" + chmod555Instruction + "' not found.",
				iuHasTouchpointDataInstruction(linuxRootIu, chmod555Instruction));
	}

	static void assertRootIuLinksMetaData(Document contentXml) {
		// global link defined in build.properties: root.link =
		// dir/file6.txt,alias_file6.txt
		Set<Element> globalFeatureRootIus = findIU(contentXml, "prf.feature_root");

		String expectedGlobalTouchpointDataInstruction = "ln(linkTarget:dir/file6.txt,targetDir:${installFolder},linkName:alias_file6.txt);";

		assertTrue(
				"Expected link (ln) touchpointData instruction '" + expectedGlobalTouchpointDataInstruction
						+ "' not found.",
				iuHasTouchpointDataInstruction(globalFeatureRootIus.iterator().next(),
						expectedGlobalTouchpointDataInstruction));

		// specific link defined in build.properties: root.linux.gtk.x86_64.link =
		// file1.txt,alias_file1.txt
		Set<Element> specificRootfeatureIus = findIU(contentXml, "prf.feature_root.gtk.linux.x86_64");

		String expectedSpecificTouchpointDataInstruction = "ln(linkTarget:file1.txt,targetDir:${installFolder},linkName:alias_file1.txt);";

		assertTrue(
				"Expected link (ln) touchpointData instruction '" + expectedSpecificTouchpointDataInstruction
						+ "' not found.",
				iuHasTouchpointDataInstruction(specificRootfeatureIus.iterator().next(),
						expectedSpecificTouchpointDataInstruction));
	}

	private static Document openMetadataRepositoryDocument(File repositoryTargetDirectory)
			throws IOException, ZipException {

		File contentJar = new File(repositoryTargetDirectory, "content.jar");
		assertTrue("content.jar not found \n" + contentJar.getAbsolutePath(), contentJar.isFile());

		return openXmlFromZip(contentJar, "content.xml");
	}

	private static Document openXmlFromZip(File zipFile, String xmlFile) throws IOException, ZipException {
		XMLParser parser = new XMLParser();
		try (ZipFile zip = new ZipFile(zipFile)) {
			ZipEntry contentXmlEntry = zip.getEntry(xmlFile);
			InputStream entryStream = zip.getInputStream(contentXmlEntry);
			return parser.parse(new XMLIOSource(entryStream));
		}
	}

	private static boolean containsIUWithProperty(Document contentXML, String iuId, String propName, String propValue) {
		Set<Element> ius = findIU(contentXML, iuId);
		for (Element unitElement : ius) {
			if (iuHasProperty(unitElement, propName, propValue))
				return true;
		}
		return false;
	}

	private static Set<Element> findIU(Document contentXML, String iuId) {
		Set<Element> foundIUs = new HashSet<>();

		Element repository = contentXML.getRootElement();
		for (Element unit : repository.getChild("units").getChildren("unit")) {
			if (iuId.equals(unit.getAttributeValue("id"))) {
				foundIUs.add(unit);
			}
		}
		return foundIUs;
	}

	private static boolean iuHasProperty(Element unit, String propName, String propValue) {
		boolean foundIU = false;

		if (propName != null) {
			for (Element property : unit.getChild("properties").getChildren("property")) {
				if (propName.equals(property.getAttributeValue("name"))
						&& propValue.equals((property.getAttributeValue("value")))) {
					foundIU = true;
					break;
				}
			}
		} else {
			foundIU = true;
		}
		return foundIU;
	}

	private static boolean iuHasAllRequirements(Element unit, String... requiredIus) {
		boolean hasAllRequirements = true;
		for (String requiredIu : requiredIus) {
			boolean foundIU = false;
			for (Element property : unit.getChild("requires").getChildren("required")) {
				if (requiredIu.equals(property.getAttributeValue("name"))) {
					foundIU = true;
					break;
				}
			}
			if (!foundIU) {
				hasAllRequirements = false;
				break;
			}
		}
		return hasAllRequirements;
	}

	private static boolean iuHasTouchpointDataInstruction(Element unit, String instructionTrimmedText) {
		Element touchpointDataElem = unit.getChild("touchpointData");

		if (touchpointDataElem != null) {
			for (Element instructions : touchpointDataElem.getChildren("instructions")) {
				for (Element instruction : instructions.getChildren("instruction")) {
					if (instructionTrimmedText.equals(instruction.getTrimmedText())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
