/*******************************************************************************
 * Copyright (c) 2011, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.eeProfile;

import static org.junit.Assert.assertThrows;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

// tests that the dependency resolver resolves for the configured execution environment (bug 364095)
public class DependencyResolverEETest extends AbstractTychoIntegrationTest {

	@Test
	public void eeFromBREE() throws Exception {
		Verifier verifier = getVerifier("/eeProfile/ee-from-bree", false);
		verifier.addCliOption(
				"-Djavax.xml-repo=" + ResourceUtil.resolveTestResource("repositories/javax.xml").toURI().toString());
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void eeFromPOM() throws Exception {
		Verifier verifier = getVerifier("/eeProfile/ee-from-pom", false);
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void breeForDependencyHigherThanCurrentBREE() throws Exception {
		Verifier verifier = getVerifier("/eeProfile/dependencyHigherBREE", false);
		verifier.setSystemProperty("resolveWithConstraints", "false");
		verifier.executeGoal("verify");
		verifier.verifyErrorFreeLog();
	}

	@Test
	public void breeForDependencyHigherThanCurrentBREEAndConstraints() throws Exception {
		Verifier verifier = getVerifier("/eeProfile/dependencyHigherBREE", false);
		verifier.setSystemProperty("resolveWithConstraints", "true");
		assertThrows(VerificationException.class, () -> verifier.executeGoal("verify"));
		verifier.verifyTextInLog(
				"requires Execution Environment that matches (&(osgi.ee=JavaSE)(version=1.8)) but the current resolution context uses [a.jre.javase 1.6.0]");
	}

}
