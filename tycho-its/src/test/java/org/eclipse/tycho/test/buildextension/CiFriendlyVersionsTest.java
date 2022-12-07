package org.eclipse.tycho.test.buildextension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class CiFriendlyVersionsTest extends AbstractTychoIntegrationTest {

	@Test
	public void testDefaultBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliArgument("-Dtycho.buildqualifier.format=yyyy");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0." + year + ".jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());

	}

	@Test
	public void testJgitBuildQualifier() throws Exception {
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		// this used the default build qualifier
		verifier.addCliArgument("-Dtycho.buildqualifier.provider=jgit");
		verifier.addCliArgument("-Dtycho.buildqualifier.format=yyyyMM");
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		// XXX Must be updated if the test is changed but should remain constant
		// otherwise as this is the git timestamp!
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
		File targetDir = new File(verifier.getBasedir(), "bundle/target");
		String[] jarFiles = targetDir.list((dir, name) -> name.endsWith(".jar"));
		assertEquals(1, jarFiles.length);
		File file = new File(targetDir, jarFiles[0]);
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
		String qualifier = jarFiles[0].substring(13, jarFiles[0].lastIndexOf('.'));
		// if formatter fails to parse it will throw exception and thus fail the test
		formatter.parse(qualifier);
	}

	@Test
	public void testWithSnapshotBuildQualifier() throws Exception {
		// building with nothing should result in the default -SNAPSHOT build
		Verifier verifier = getVerifier("ci-friendly/buildqualifier", false, true);
		verifier.executeGoals(List.of("clean", "package"));
		verifier.verifyErrorFreeLog();
		File file = new File(verifier.getBasedir(), "bundle/target/bundle-1.0.0-SNAPSHOT.jar");
		assertTrue(file.getAbsolutePath() + " is not generated!", file.isFile());
	}
}
