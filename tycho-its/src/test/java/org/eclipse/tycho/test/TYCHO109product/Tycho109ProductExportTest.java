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
package org.eclipse.tycho.test.TYCHO109product;

import static org.eclipse.tycho.test.util.EnvironmentUtil.isEclipse32Platform;

import java.io.File;

import junit.framework.Assert;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class Tycho109ProductExportTest extends AbstractTychoIntegrationTest {

    @Test
    public void exportFeatureProduct() throws Exception {
        Verifier verifier;
        if (isEclipse32Platform()) {
            verifier = getVerifier("/TYCHO109product/eclipse32/feature-rcp");
        } else {
            verifier = getVerifier("/TYCHO109product/feature-rcp");
        }

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());
        File output = new File(basedir, "HeadlessProduct/target/linux.gtk.x86_64/eclipse");

        Assert.assertTrue("Exported product folder not found\n" + output.getAbsolutePath(), output.isDirectory());
        File launcher = new File(output, "launcher");
        Assert.assertTrue("Launcher not found\n" + launcher, launcher.isFile());
        Assert.assertTrue("config.ini not found", new File(output, "configuration/config.ini").isFile());

        File plugins = new File(output, "plugins");
        Assert.assertTrue("Plugins folder not found", plugins.isDirectory());
        // On linux the number is not same, can't rely on that
        /*
         * Assert.assertTrue("No found the expected plugins number", 324, plugins.list().length);
         */

        //MNGECLIPSE-974
        File headlessPlugin = new File(plugins, "HeadlessPlugin_1.0.0");
        Assert.assertTrue("Plugin should be unpacked", headlessPlugin.isDirectory());

        File features = new File(output, "features");
        Assert.assertTrue("Features folder not found", features.isDirectory());
        // On linux the number is not same, can't rely on that
        /*
         * Assert.assertEquals("No found the expected features number", 18, features.list().length);
         */

        // launch to be sure
//		Commandline cmd = new Commandline();
//		cmd.setExecutable(launcher.getAbsolutePath());
//
//		StringWriter logWriter = new StringWriter();
//		StreamConsumer out = new WriterStreamConsumer(logWriter);
//		StreamConsumer err = new WriterStreamConsumer(logWriter);
//		CommandLineUtils.executeCommandLine(cmd, out, err);
//		Assert.assertTrue("Didn't get a controlled exit\n"
//				+ logWriter.toString(), logWriter.toString().startsWith(
//				"Headless application OK!"));
    }

//	private File getLauncher(File output, String expectedName) {
//		if (expectedName == null) {
//			expectedName = "launcher";
//		}
//		if (isWindows()) {
//			return new File(output, expectedName + ".exe");
//		} else if (isLinux()) {
//			return new File(output, expectedName);
//		} else if (isMac()) {
//			return new File(output, "Eclipse.app/Contents/MacOS/"
//					+ expectedName);
//		} else {
//			Assert.fail("Unable to determine launcher to current OS");
//			return null;
//		}
//	}

    @Test
    public void exportPluginRcpApplication() throws Exception {
        if (isEclipse32Platform()) {
            // regression test for TYCHO-199, no need to verify on e32 
            return;
        }

        Verifier verifier = getVerifier("/TYCHO109product/plugin-rcp-app");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void productNoZip() throws Exception {
        if (isEclipse32Platform()) {
            // regression test for TYCHO-199, no need to verify on e32 
            return;
        }

        Verifier verifier = getVerifier("/TYCHO109product/product-nozip/product");
        verifier.getSystemProperties().setProperty("tycho.product.createArchive", "false");

        verifier.executeGoal("package");
        verifier.verifyErrorFreeLog();

        File basedir = new File(verifier.getBasedir());
        Assert.assertFalse(new File(basedir, "product/target/product-1.0.0.zip").canRead());
    }
}
