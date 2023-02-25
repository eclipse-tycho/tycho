/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.util.EnvironmentUtil;
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
        File dst = new File("target/projects",
                getClass().getSimpleName() + "/" + name.getMethodName() + "/" + test.replace("../", "./"))
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
        //Test JVM can be started in debug mode by passing the following property to the maven run:
        //-Dtycho.mvnDebug -> will start with port 8000
        //-Dtycho.mvnDebug=12345 -> will start with port 12345
        //-Dtycho.mvnDebug==-<any custom options>

        // oddly enough, Verifier uses this system property to locate maven install
        System.setProperty("maven.home", getMavenHome());

        File testDir = getBasedir(test);

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setForkJvm(true);
        String debug = System.getProperty("tycho.mvnDebug");
        if (debug != null) {
            System.out.println("Preparing to execute Maven in debug mode");
            String mvnOpts;
            if (debug.startsWith("-")) {
                //use as is...
                mvnOpts = debug;
                System.out.println("Using custom debug-opts: " + mvnOpts);
            } else {
                int port;
                try {
                    port = Integer.parseInt(debug);
                } catch (RuntimeException e) {
                    port = 8000;
                }
                mvnOpts = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + port;
                System.out.println("Listening for transport dt_socket at address: " + port);
            }
            verifier.getEnvironmentVariables().put("MAVEN_OPTS", mvnOpts);
            System.out.flush();
        }
        verifier.addCliArgument("-Dmaven.home=" + getMavenHome());
        verifier.addCliArgument("-Dtycho-version=" + getTychoVersion());
        // bug 447397: use temp dir in target/ folder to make sure we don't leave garbage behind
        // when using maven < 3.1 
        File tmpDir = new File("target/tmp");
        tmpDir.mkdirs();
        verifier.addCliArgument("-Djava.io.tmpdir=" + tmpDir.getAbsolutePath());
        if (setTargetPlatform) {
            verifier.addCliArgument("-Dtarget-platform=" + getTargetPlatform());
        }
        if (ignoreLocalArtifacts) {
            verifier.addCliArgument("-Dtycho.localArtifacts=ignore");
        }
        verifier.addCliArgument("-X");
        verifier.addCliArguments("-s", userSettings.getAbsolutePath());
        verifier.getVerifierProperties().put("use.mavenRepoLocal", "true");
        verifier.setLocalRepo(EnvironmentUtil.getLocalRepo());

        String customOptions = System.getProperty("it.cliOptions");
        if (customOptions != null && !customOptions.trim().isEmpty()) {
            verifier.addCliArguments(customOptions.split(" "));
        }

        if (System.getProperty(SYSPROP_STATELOCATION) != null) {
            verifier.setForkJvm(false);
            String m2eresolver = System.getProperty("tychodev-maven.ext.class.path"); // XXX
            if (m2eresolver != null) {
                verifier.addCliArgument("-Dmaven.ext.class.path=" + m2eresolver);
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

    /**
     * Check that at least one file matching the given pattern exits and returns an array of
     * matching files
     * 
     * @param baseDir
     *            the base directory to scan
     * @param pattern
     *            the pattern to match
     * @return an array of matching files (will contain at least one file)
     */
    protected File[] assertFileExists(File baseDir, String pattern) {
        DirectoryScanner ds = scan(baseDir, pattern);
        File[] includedFiles = Arrays.stream(ds.getIncludedFiles()).map(file -> new File(baseDir, file))
                .toArray(File[]::new);
        assertEquals(baseDir.getAbsolutePath() + "/" + pattern, 1, includedFiles.length);
        assertTrue(baseDir.getAbsolutePath() + "/" + pattern, includedFiles[0].canRead());
        return includedFiles;
    }

    protected void assertDirectoryExists(File targetdir, String pattern) {
        DirectoryScanner ds = scan(targetdir, pattern);
        assertEquals(targetdir.getAbsolutePath() + "/" + pattern, 1, ds.getIncludedDirectories().length);
        assertTrue(targetdir.getAbsolutePath() + "/" + pattern,
                new File(targetdir, ds.getIncludedDirectories()[0]).exists());
    }

    protected void assertFileDoesNotExist(File targetdir, String pattern) {
        DirectoryScanner ds = scan(targetdir, pattern);
        assertEquals(targetdir.getAbsolutePath() + "/" + pattern, 0, ds.getIncludedFiles().length);
    }

    protected void assertDirectoryDoesNotExist(File baseDir, String pattern) {
        DirectoryScanner ds = scan(baseDir, pattern);
        assertEquals(baseDir.getAbsolutePath() + "/" + pattern, 0, ds.getIncludedDirectories().length);
    }

    private static DirectoryScanner scan(File targetdir, String pattern) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(targetdir);
        ds.setIncludes(new String[] { pattern });
        ds.scan();
        return ds;
    }

    protected String toURI(File file) throws IOException {
        return file.getCanonicalFile().toURI().normalize().toString();
    }

    public static void verifyTextInLogMatches(Verifier verifier, Pattern pattern) throws VerificationException {
        List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);

        for (String line : lines) {
            if (pattern.matcher(Verifier.stripAnsi(line)).find()) {
                return;
            }
        }
        throw new VerificationException("Pattern not found in log: " + pattern);
    }

    public static void verifyTextNotInLog(Verifier verifier, String text) throws VerificationException {
        List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);

        for (String line : lines) {
            if (Verifier.stripAnsi(line).contains(text)) {
                throw new VerificationException("Text '" + text + "' was found in the log!");
            }
        }
    }

}
