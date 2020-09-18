package org.eclipse.tycho.test.packaging;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

// tests the support for reproducible artifacts (bug 362883 - "do not generate new artifact unless there is a change")
public class BaselineValidateAndReplaceTest extends AbstractTychoIntegrationTest {

    private static File baselineRepo;

    @BeforeClass
    public static void setupClass() throws IOException {
        baselineRepo = new File("projects/packaging.reproducibleArtifacts/baseline/repository").getCanonicalFile();
    }

    private Verifier getVerifier(String project, File baselineRepo) throws Exception {
        Verifier verifier = getVerifier("/packaging.reproducibleArtifacts/" + project, false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.getCliOptions().add("-Dbaseline-repo=" + baselineRepo.toURI().toString());
        return verifier;
    }

    private void assertBaselineContents(File repository, String path) throws IOException {
        Assert.assertTrue(isBaselineContents(repository, path));
    }

    private void assertReactorContents(File repository, String path) throws IOException {
        Assert.assertFalse(isBaselineContents(repository, path));
    }

    private boolean isBaselineContents(File repository, String path) throws IOException {
        File file = new File(repository, path);

        Assert.assertTrue(file.exists());

        File baselineFile = new File(baselineRepo, path);
        return FileUtils.contentEquals(baselineFile, file);
    }

    @Test
    public void testBaselineRepositoryDoesNotExist() throws Exception {
        // likely initial state is when baseline repository url points at empty or garbage location
        File notARepository = new File("baseline/src").getCanonicalFile();
        Verifier verifier = getVerifier("baseline/src", notARepository);

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testRebuildOfTheSameCodebase() throws Exception {
        Verifier verifier = getVerifier("baseline/src", baselineRepo);

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        File repository = new File(verifier.getBasedir(), "repository/target/repository");

        assertBaselineContents(repository, "features/baseline.feature01_1.0.0.1.jar");
        assertBaselineContents(repository, "plugins/baseline.bundle01_1.0.0.1.jar");
        assertBaselineContents(repository, "plugins/baseline.bundle01.source_1.0.0.1.jar");
    }

    @Test
    public void testNewVersion() throws Exception {
        Verifier verifier = getVerifier("baseline/src", baselineRepo);
        verifier.getCliOptions().add("-DversionQualifier=2");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();

        File repository = new File(verifier.getBasedir(), "repository/target/repository");

        assertFileDoesNotExist(repository, "features/baseline.feature01_1.0.0.1.jar");
        assertFileDoesNotExist(repository, "plugins/baseline.bundle01_1.0.0.1.jar");
        assertFileDoesNotExist(repository, "plugins/baseline.bundle01.source_1.0.0.1.jar");

        assertFileExists(repository, "features/baseline.feature01_1.0.0.2.jar");
        assertFileExists(repository, "plugins/baseline.bundle01_1.0.0.2.jar");
        assertFileExists(repository, "plugins/baseline.bundle01.source_1.0.0.2.jar");
    }

    @Test
    public void testCorruptedBaselineRepo() throws Exception {
        File corruptedBaselineRepo = new File("projects/packaging.reproducibleArtifacts/baseline/repository_corrupted")
                .getCanonicalFile();
        Verifier verifier = getVerifier("baseline/src", corruptedBaselineRepo);
        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
            Assert.fail("should not reach here");
        } catch (VerificationException expected) {
        }
        File locallyBuiltJar = new File(verifier.getBasedir(), "bundle01/target/baseline.bundle01-1.0.0-SNAPSHOT.jar");
        Assert.assertTrue(locallyBuiltJar.isFile());
        // locally built jar must not be replaced with corrupted 0-byte baseline jar
        Assert.assertTrue(locallyBuiltJar.length() > 0);
        verifier.verifyTextInLog("Error trying to download baseline.bundle01 version 1.0.0.1");
    }

    @Test
    public void testContentChangedStrict() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testBaselineDisable() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=disable");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testBaselineWarn() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=warn");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testBaselineWarn_changedAttachedArtifact() throws Exception {
        Verifier verifier = getVerifier("changedattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=warn");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");

        File repository = new File(verifier.getBasedir(), "repository/target/repository");
        assertBaselineContents(repository, "plugins/baseline.bundle01.source_1.0.0.1.jar");
    }

    @Test
    public void testBaselineFailCommon_Changed() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=failCommon");

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }

        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testBaselineFailCommon_Changed_ignoredFiles() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);
        verifier.getCliOptions().addAll(Arrays.asList("--projects", "bundle01"));
        verifier.getCliOptions().add("-PignoreChanged");
        verifier.getCliOptions().add("-Dtycho.baseline=failCommon");
        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testBaselineFailCommon_newAttachedArtifact() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=failCommon");

        verifier.executeGoals(Arrays.asList("clean", "package"));

        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testBaselineFail() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=fail");

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }

        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testBaselineFail_changedAttachedArtifact() throws Exception {
        Verifier verifier = getVerifier("changedattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=fail");

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");
    }

    @Test
    public void testReplaceNone() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=warn");
        verifier.getCliOptions().add("-Dtycho.baseline.replace=none");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");

        File repository = new File(verifier.getBasedir(), "repository/target/repository");
        assertReactorContents(repository, "plugins/baseline.bundle01_1.0.0.1.jar");
    }

    @Test
    public void testReplaceCommon() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=warn");
        verifier.getCliOptions().add("-Dtycho.baseline.replace=common");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");

        File repository = new File(verifier.getBasedir(), "repository/target/repository");
        assertBaselineContents(repository, "features/baseline.feature02_1.0.0.1.jar");

        File basedir = new File(verifier.getBasedir());
        assertFileExists(basedir, "feature02/target/baseline.feature02_root-1.0.0.1-root.zip");
        assertFileDoesNotExist(basedir, "repository/target/repository/binary/baseline.feature02_root_1.0.0.1");
    }

    @Test
    public void testReplaceAll() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline=warn");
        verifier.getCliOptions().add("-Dtycho.baseline.replace=all");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and build artifacts have same version but different contents");

        File repository = new File(verifier.getBasedir(), "repository/target/repository");
        assertBaselineContents(repository, "features/baseline.feature02_1.0.0.1.jar");

        File basedir = new File(verifier.getBasedir());
        assertFileDoesNotExist(basedir, "feature02/target/baseline.feature02_root-1.0.0.1-root.zip");
        assertFileDoesNotExist(basedir, "repository/target/repository/binary/baseline.feature02_root_1.0.0.1");
        // TODO ideally also verify artifacts are detached from the project and are not installed/deployed
    }

}
