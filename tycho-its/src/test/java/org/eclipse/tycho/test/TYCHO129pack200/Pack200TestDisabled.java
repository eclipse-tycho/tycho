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
package org.eclipse.tycho.test.TYCHO129pack200;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

/* java -jar \eclipse\plugins\org.eclipse.equinox.launcher_1.0.1.R33x_v20080118.jar -application org.eclipse.update.core.siteOptimizer -digestBuilder -digestOutputDir=d:\temp\eclipse\digest -siteXML=D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site\site.xml  -jarProcessor -processAll -pack -outputDir d:\temp\eclipse\site D:\sonatype\workspace\tycho\tycho-its\projects\tycho129\tycho.demo.site\target\site */
public class Pack200TestDisabled extends AbstractTychoIntegrationTest {

    //@Test
    public void generatePackSite() throws Exception {
        Verifier verifier = getVerifier("/tycho129");
        verifier.setAutoclean(false);

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File plugin = new File(verifier.getBasedir(),
                "tycho.demo.site/target/site/plugins/tycho.demo_1.0.0.jar.pack.gz");
        Assert.assertTrue("Plugin pack should exist " + plugin, plugin.exists());
    }

}
