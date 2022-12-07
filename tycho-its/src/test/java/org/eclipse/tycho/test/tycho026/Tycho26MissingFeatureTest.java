/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.test.tycho026;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/* java -jar \eclipse\plugins\org.eclipse.equinox.launcher_1.0.1.R33x_v20080118.jar -application org.eclipse.update.core.siteOptimizer -digestBuilder -digestOutputDir=d:\temp\eclipse\digest -siteXML=D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site\site.xml  -jarProcessor -processAll -pack -outputDir d:\temp\eclipse\site D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site */
public class Tycho26MissingFeatureTest extends AbstractTychoIntegrationTest {

	@Test(expected = VerificationException.class)
	public void test() throws Exception {
		Verifier verifier = getVerifier("/tycho026");
		verifier.setAutoclean(false);

		verifier.executeGoal("package");
		verifier.verifyErrorFreeLog();
	}

}
