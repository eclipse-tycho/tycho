package org.eclipse.tycho.test.apitools;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.XMLParser;

public class ApiToolsTest extends AbstractTychoIntegrationTest {
	@Test
	public void testGenerate() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File descriptionFile = new File(verifier.getBasedir(), "bundle/target/.api_description");
		assertTrue(descriptionFile.getAbsoluteFile() + " not found", descriptionFile.isFile());
		Document document = XMLParser.parse(descriptionFile);
		assertEquals("api-bundle_0.0.1-SNAPSHOT", document.getRootElement().getAttribute("name").getValue());
		// TODO enhance project and assert more useful things...
	}

	@Test
	public void testAnalyse() throws Exception {
		Verifier verifier = getVerifier("api-tools", true, true);
		verifier.executeGoals(List.of("clean", "verify"));
		List<String> logLines = Files.readAllLines(Path.of(verifier.getBasedir(), verifier.getLogFileName()));
		// TODO: check that there are some API-errors
		// TODO: check with api-filter
		// TODO: check with second plugin with BREE?

	}
}
