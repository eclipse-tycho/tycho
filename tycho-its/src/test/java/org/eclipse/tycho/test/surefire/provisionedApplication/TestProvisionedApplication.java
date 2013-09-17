/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat JBoss) - Test
 *******************************************************************************/
package org.eclipse.tycho.test.surefire.provisionedApplication;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.EnvironmentUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProvisionedApplication extends AbstractTychoIntegrationTest {

    private Verifier projectVerifier = null;
    private File productModule;

    private List<String> defaultCliOptions;
    private Map<?, ?> defaultSystemProperties;

    @Before
    public void setUp() throws Exception {
        this.projectVerifier = getVerifier("surefire.provisionedApplication", false);

        this.defaultCliOptions = Collections.unmodifiableList((List<String>) this.projectVerifier.getCliOptions());
        this.defaultSystemProperties = Collections.unmodifiableMap(this.projectVerifier.getSystemProperties());

        this.productModule = new File(this.projectVerifier.getBasedir(), "p2-director-product");

        List<String> cliOptions = new ArrayList<String>(this.defaultCliOptions);
        cliOptions.add("-f");
        cliOptions.add(new File(this.productModule, "pom.xml").getAbsolutePath());
        this.projectVerifier.setCliOptions(cliOptions);

        this.projectVerifier.setCliOptions(cliOptions);
        this.projectVerifier.executeGoal("install");
        this.projectVerifier.verifyErrorFreeLog();
    }

    // @Test TODO
    public void testProvisionAppAndRunTest() throws Exception {
        File moduleLocation = new File(this.projectVerifier.getBasedir(), "testProvisionApplication");

        List<String> cliOptions = new ArrayList<String>(this.defaultCliOptions);
        cliOptions.add("-f");
        cliOptions.add(new File(moduleLocation, "pom.xml").getAbsolutePath());
        this.projectVerifier.setCliOptions(cliOptions);

        Properties systemProperties = new Properties();
        systemProperties.putAll(this.defaultSystemProperties);
        systemProperties.put("product-repo-url", new File(this.productModule, "target/repository").getAbsolutePath());
        this.projectVerifier.setSystemProperties(systemProperties);

        this.projectVerifier.executeGoal("verify");
        this.projectVerifier.verifyErrorFreeLog();

        File applicationLocation = new File(moduleLocation, "target/p2temp");
        Assert.assertTrue("Application not provisionned", applicationLocation.isDirectory());
        File pluginsDirectory = new File(applicationLocation, "plugins");
        Assert.assertEquals("Test bundle not installed in target application", 1,
                pluginsDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(File arg0, String arg1) {
                        return arg1.startsWith("testProvisionApplication");
                    }
                }).length);
        File markerFile = new File(pluginsDirectory, "testRanThere");
        Assert.assertTrue("Test did not run on expected application", markerFile.exists());
    }

    @Test
    public void testRunTestOnProvisionedApp() throws Exception {
        this.projectVerifier.setDebugJvm(true); // TODO
        StringBuilder productPath = new StringBuilder();
        if (EnvironmentUtil.isLinux()) {
            productPath.append("linux/gtk/");
        } else if (EnvironmentUtil.isWindows()) {
            productPath.append("win32/win32/");
        } else if (EnvironmentUtil.isMac()) {
            productPath.append("macosx/cocoa/");
        }
        productPath.append(System.getProperty("os.arch").contains("64") ? "x86_64" : "x86");
        File applicationLocation = new File(this.productModule, "target/products/p2-director-product/"
                + productPath.toString());
        File pluginsDirectory = new File(applicationLocation, "plugins");
        Assert.assertEquals("application already contains test bundle", 0,
                pluginsDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(File arg0, String arg1) {
                        return arg1.startsWith("testProvisionedApplication");
                    }
                }).length);

        File moduleLocation = new File(this.projectVerifier.getBasedir(), "testProvisionedApplication");

        List<String> cliOptions = new ArrayList<String>(this.defaultCliOptions);
        cliOptions.add("-f");
        cliOptions.add(new File(moduleLocation, "pom.xml").getAbsolutePath());
        this.projectVerifier.setCliOptions(cliOptions);

        Properties systemProperties = new Properties();
        systemProperties.putAll(this.defaultSystemProperties);
        systemProperties.put("tycho.testProvisionedApplication", applicationLocation.getAbsolutePath());
        this.projectVerifier.setSystemProperties(systemProperties);

        this.projectVerifier.executeGoal("verify");
        this.projectVerifier.verifyErrorFreeLog();

        Assert.assertEquals("Test bundle not installed in target application", 1,
                pluginsDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(File arg0, String arg1) {
                        return arg1.startsWith("testProvisionedApplication");
                    }
                }).length);
        File markerFile = new File(pluginsDirectory, "testRanThere");
        Assert.assertTrue("Test did not run on expected application", markerFile.exists());
    }

    @After
    public void restoreDefault() {
        this.projectVerifier.setCliOptions(this.defaultCliOptions);
        Properties defaultProperties = new Properties();
        defaultProperties.putAll(this.defaultSystemProperties);
        this.projectVerifier.setSystemProperties(defaultProperties);
    }
}
