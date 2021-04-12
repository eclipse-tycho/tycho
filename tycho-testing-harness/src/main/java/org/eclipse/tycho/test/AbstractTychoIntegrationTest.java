/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.test;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.util.EnvironmentUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class AbstractTychoIntegrationTest {

    /**
     * Location of m2e.tycho workspace state location.
     * <p/>
     * Value must match among tycho-insitu, DevelopmentWorkspaceState and
     * AbstractTychoIntegrationTest.
     */
    private static final String SYSPROP_STATELOCATION = "tychodev.workspace.state";

    @Rule
    public TestName name = new TestName();

    protected File getBasedir(String test) throws IOException {
        File src = new File("projects", test).getAbsoluteFile();
        File dst = new File("target/projects", getClass().getSimpleName() + "/" + name.getMethodName() + "/" + test)
                .getAbsoluteFile();

        if (dst.isDirectory()) {
            FileUtils.deleteDirectory(dst);
        } else if (dst.isFile()) {
            if (!dst.delete()) {
                throw new IOException("Can't delete file " + dst.toString());
            }
        }

        FileUtils.copyDirectoryStructure(src, dst);

        return dst;
    }

    protected Verifier getVerifier(String test, boolean setTargetPlatform) throws Exception {
        return getVerifier(test, setTargetPlatform, getSettings());
    }

    protected Verifier getVerifier(String test, boolean setTargetPlatform, boolean ignoreLocalArtifacts)
            throws Exception {
        return getVerifier(test, setTargetPlatform, getSettings(), ignoreLocalArtifacts);
    }

    protected Verifier getVerifier(String test, boolean setTargetPlatform, File userSettings) throws Exception {
        return getVerifier(test, setTargetPlatform, userSettings, true);
    }

    protected Verifier getVerifier(String test, boolean setTargetPlatform, File userSettings,
            boolean ignoreLocalArtifacts) throws Exception {
        /*
         * Test JVM can be started in debug mode by passing the following env to execute(...)
         * methods.
         * 
         * java.util.Map<String, String> env = new java.util.HashMap<String, String>();
         * env.put("MAVEN_OPTS",
         * "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
         */

        // oddly enough, Verifier uses this system property to locate maven install
        System.setProperty("maven.home", getMavenHome());

        File testDir = getBasedir(test);

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.getCliOptions().add("-Dmaven.home=" + getMavenHome());
        verifier.getCliOptions().add("-Dtycho-version=" + getTychoVersion());
        // bug 447397: use temp dir in target/ folder to make sure we don't leave garbage behind
        // when using maven < 3.1 
        File tmpDir = new File("target/tmp");
        tmpDir.mkdirs();
        verifier.getCliOptions().add("-Djava.io.tmpdir=" + tmpDir.getAbsolutePath());
        if (setTargetPlatform) {
            verifier.getCliOptions().add("-Dtarget-platform=" + getTargetPlatform());
        }
        if (ignoreLocalArtifacts) {
            verifier.getCliOptions().add("-Dtycho.localArtifacts=ignore");
        }
        verifier.getCliOptions().add("-X");
        verifier.getCliOptions().add("-s " + userSettings.getAbsolutePath());
        verifier.getVerifierProperties().put("use.mavenRepoLocal", "true");
        verifier.setLocalRepo(EnvironmentUtil.getLocalRepo());

        String customOptions = System.getProperty("it.cliOptions");
        if (customOptions != null && !customOptions.trim().isEmpty()) {
            verifier.getCliOptions().add(customOptions);
        }

        if (System.getProperty(SYSPROP_STATELOCATION) != null) {
            verifier.setForkJvm(false);
            String m2eresolver = System.getProperty("tychodev-maven.ext.class.path"); // XXX
            if (m2eresolver != null) {
                verifier.addCliOption("-Dmaven.ext.class.path=" + m2eresolver);
            }
        }

        return verifier;

    }

    protected Verifier getVerifier(String test) throws Exception {
        return getVerifier(test, true);
    }

    protected String getTargetPlatform() {
        return EnvironmentUtil.getTargetPlatform();
    }

    private static File getSettings() {
        // alternative settings.xml, e.g. outside the source code repository
        // - read from Eclipse launch configuration
        String systemValue = System.getProperty("tycho.testSettings");
        if (systemValue != null) {
            return new File(systemValue);
        }

        // - read from command line
        String commandLineValue = EnvironmentUtil.getTestSettings();
        if (commandLineValue != null) {
            return new File(commandLineValue);
        }

        // default: settings.xml in the root of the integration test project (e.g. tycho-its/settings.xml)
        return new File("settings.xml");
    }

    protected String getMavenHome() {
        String mavenHome = EnvironmentUtil.getMavenHome();
        if (mavenHome == null) {
            throw new IllegalStateException(
                    "Generated test data for the integration tests is missing. Run the launch configuration 'tycho-its - prepare test resources' first.");
        }
        return mavenHome;
    }

    protected String getTychoVersion() {
        return EnvironmentUtil.getTychoVersion();
    }

    protected void assertFileExists(File targetdir, String pattern) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(targetdir);
        ds.setIncludes(pattern);
        ds.scan();
        Assert.assertEquals(targetdir.getAbsolutePath() + "/" + pattern, 1, ds.getIncludedFiles().length);
        Assert.assertTrue(targetdir.getAbsolutePath() + "/" + pattern,
                new File(targetdir, ds.getIncludedFiles()[0]).canRead());
    }

    protected void assertDirectoryExists(File targetdir, String pattern) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(targetdir);
        ds.setIncludes(pattern);
        ds.scan();
        Assert.assertEquals(targetdir.getAbsolutePath() + "/" + pattern, 1, ds.getIncludedDirectories().length);
        Assert.assertTrue(targetdir.getAbsolutePath() + "/" + pattern,
                new File(targetdir, ds.getIncludedDirectories()[0]).exists());
    }

    protected void assertFileDoesNotExist(File targetdir, String pattern) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(targetdir);
        ds.setIncludes(pattern);
        ds.scan();
        Assert.assertEquals(targetdir.getAbsolutePath() + "/" + pattern, 0, ds.getIncludedFiles().length);
    }

    protected String toURI(File file) throws IOException {
        return file.getCanonicalFile().toURI().normalize().toString();
    }

}
