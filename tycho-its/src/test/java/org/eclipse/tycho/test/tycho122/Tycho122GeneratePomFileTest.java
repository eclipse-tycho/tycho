/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.tycho122;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho122GeneratePomFileTest extends AbstractTychoIntegrationTest {
    @Test
    public void generatePom() throws Exception {
        Verifier verifier = getVerifier("/tycho122/tycho.demo");

        verifier.setAutoclean(false);
        verifier.executeGoal("org.eclipse.tycho:tycho-pomgenerator-plugin:generate-poms");
        verifier.verifyErrorFreeLog();

        File pom = new File(verifier.getBasedir(), "pom.xml");
        Assert.assertTrue("Must generate the pom.xml", pom.exists());
    }

}
