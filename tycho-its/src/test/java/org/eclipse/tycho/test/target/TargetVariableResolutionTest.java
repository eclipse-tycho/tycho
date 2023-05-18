package org.eclipse.tycho.test.target;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.HttpServer;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TargetVariableResolutionTest extends AbstractTychoIntegrationTest {
	private HttpServer server;
	private String baseurl;

	@Before
	public void startServer() throws Exception {
		server = HttpServer.startServer();
		server.addServer("repo", ResourceUtil.resolveTestResource("repositories/javax.xml"));
		var urlWithContextPath = server.getUrl("");
		baseurl = urlWithContextPath.endsWith("/") // double slash causes trouble in RepositoryTransport.download
				? urlWithContextPath.substring(0, urlWithContextPath.length() - 1)
				: urlWithContextPath;
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Test
	public void repositoryUrlCanContainEnvVarVariable() throws Exception {
		Verifier verifier = getVerifier("target.variables-env", false);
		verifier.setEnvironmentVariable("MY_MIRROR", baseurl);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("validate-target-platform");
	}

	@Test
	public void repositoryUrlCanContainSystemPropertyVariable() throws Exception {
		Verifier verifier = getVerifier("target.variables-sysprop", false);
		verifier.setSystemProperty("myMirror", baseurl);
		verifier.executeGoals(Arrays.asList("package"));
		verifier.verifyErrorFreeLog();
		verifier.verifyTextInLog("validate-target-platform");
	}
}
