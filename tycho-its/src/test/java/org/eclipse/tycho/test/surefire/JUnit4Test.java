/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.surefire;

import static org.eclipse.tycho.test.util.SurefireUtil.testResultFile;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class JUnit4Test extends AbstractTychoIntegrationTest {

	@Test
	public void basicTest() throws Exception {

		// a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
		Verifier verifier = getVerifier("tycho-surefire-plugin/junit4/bundle.test");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();

		assertTrue(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit4Test").exists());

		// ensure that JUnit 3 style tests also work -> related to bug 388909
		assertTrue(testResultFile(verifier.getBasedir(), "bundle.test", "JUnit3Test").exists());

	}

	/**
	 * On old Equinox, bundles paths are not easily available, requiring heavy
	 * classpath computation logic in
	 * org.eclipse.tycho.surefire.osgibooter.OsgiSurefireBooter.
	 * 
	 */
	@Test
	public void osgibooter_on_old_equinox() throws Exception {
		Verifier verifier = getVerifier("tycho-surefire-plugin/junit4/tycho-osgibooter-cnfe-repro");
		File global_repo = new File(verifier.getLocalRepository());
		// Depending on locations of:
		// - local repository
		// - surefire installation
		// buggy Tycho surefire may produce accidentally valid result, resulting in
		// false negative for this test.
		// For example, if
		// - this test project is in /Users/user/git/tycho/tycho-its
		// - local repository is in `/tmp/fresh_dir`
		// The test will pass, as
		// `/Users/user/git/tycho/tycho-its/target/projects/JUnit4Test/osgibooter_on_old_equinox/tycho-surefire-plugin/junit4/tycho-osgibooter-cnfe-repro/target/work/configuration/config.ini`
		// would have install area
		// `/Users/user/git/tycho/tycho-its/target/projects/JUnit4Test/osgibooter_on_old_equinox/tycho-surefire-plugin/junit4/tycho-osgibooter-cnfe-repro/target/work`
		// and refer to
		// `/tmp/fresh_dir/org/eclipse/tycho/org.eclipse.tycho.surefire.osgibooter/6.0.0-SNAPSHOT/org.eclipse.tycho.surefire.osgibooter-6.0.0-SNAPSHOT.jar`
		// relative path to osbibooter from install area:
		// ../../../../../../../../../../../../../../tmp/fresh_dir/org/eclipse/tycho/org.eclipse.tycho.surefire.osgibooter/6.0.0-SNAPSHOT/org.eclipse.tycho.surefire.osgibooter-6.0.0-SNAPSHOT.jar
		// if resolved against surefire project location
		// /Users/user/git/tycho/tycho-its/target/projects/JUnit4Test/osgibooter_on_old_equinox/tycho-surefire-plugin/junit4/tycho-osgibooter-cnfe-repro
		// rejecting extra "../" becomes
		// /tmp/fresh_dir/org/eclipse/tycho/org.eclipse.tycho.surefire.osgibooter/6.0.0-SNAPSHOT/org.eclipse.tycho.surefire.osgibooter-6.0.0-SNAPSHOT.jar
		// matching the correct location by accident.
		// Such accidental match would result in a false-negative of this test.
		// The probability of false negative grows as local repository path length
		// shrinks.
		// To reduce the probability and force the test to fail, we use longer
		// repository path here.

		// Disturb location of repository in relation to Surefire installation location
		// to avoid accidental incorrect relative path matching in OsgiSurefireBooter.
		File fresh_repo = new File(verifier.getBasedir(), "fresh_local_repo");
		fresh_repo = new File("/tmp/fresh_dir");
		verifier.setLocalRepo(fresh_repo.toString());
		verifier.setCliOptions(
				List.of("-Dmy.custom.plugin.repo=" + global_repo.toURI().toString(), "--no-transfer-progress"));

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void contextClassLoaderTest() throws Exception {

		// a eclipse-test-plugin using JUnit 4 -> supported since MNGECLIPSE-1031
		Verifier verifier = getVerifier("tycho-surefire-plugin/junit4/contextclassloader.test/");

		verifier.executeGoal("integration-test");
		verifier.verifyErrorFreeLog();
		File testResultFile = testResultFile(verifier.getBasedir(), "contextclassloader.test", "JUnit4Test");
		assertTrue("Test Result File" + testResultFile + " is missing", testResultFile.exists());

	}

}
