package org.eclipse.tycho.test.target;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.eclipse.tycho.test.util.TargetDefinitionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class TargetDependenciesAccrossLocationsTest extends AbstractTychoIntegrationTest {

	private HttpServer server;
	private String repoAUrl;
	private String repoBUrl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
		repoAUrl = server.addServer("repoA",
				ResourceUtil.resolveTestResource("repositories/target.dependenciesAcrossLocations/repoA"));
		repoBUrl = server.addServer("repoB",
				ResourceUtil.resolveTestResource("repositories/target.dependenciesAcrossLocations/repoB"));
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void slicerDoesNotFailWhenDependenciesExistInDifferentTargetLocation() throws Exception {
		Verifier verifier = getVerifier("target.slicerWithDependenciesInDifferentTargetLocation", false);
		fillInTargetUrls(verifier);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void slicerDoesNotFailWhenDependenciesDoNotExistInAnyLocation() throws Exception {
		Verifier verifier = getVerifier("target.slicerWithMissingDependencies", false);
		fillInTargetUrls(verifier);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void plannerDoesNotFailWhenDependenciesExistInDifferentTargetLocation() throws Exception {
		Verifier verifier = getVerifier("target.plannerWithDependenciesInDifferentTargetLocation", false);
		fillInTargetUrls(verifier);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void plannerFailsWhenDependenciesDoNotExistInAnyLocation() throws Exception {
		Verifier verifier = getVerifier("target.plannerWithMissingDependencies", false);
		fillInTargetUrls(verifier);
		try {
			verifier.executeGoals(Arrays.asList("package"));
		} catch (VerificationException e) {
			// expected
		}
		verifier.verifyTextInLog(
				"Missing requirement: bundle2 1.0.0 requires 'osgi.bundle; bundle1 0.0.0' but it could not be found");
	}

	private void fillInTargetUrls(Verifier verifier) throws IOException, ParserConfigurationException, SAXException {
		File platformFile = new File(verifier.getBasedir(), "targetplatform/targetplatform.target");
		TargetDefinitionUtil.setRepositoryURLs(platformFile, "repoA", repoAUrl);
		TargetDefinitionUtil.setRepositoryURLs(platformFile, "repoB", repoBUrl);
	}
}