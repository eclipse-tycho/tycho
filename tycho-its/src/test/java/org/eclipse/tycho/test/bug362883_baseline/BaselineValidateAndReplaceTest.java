package org.eclipse.tycho.test.bug362883_baseline;

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

public class BaselineValidateAndReplaceTest extends AbstractTychoIntegrationTest {

    private static File baselineRepo;

    @BeforeClass
    public static void setupClass() throws IOException {
        baselineRepo = new File("projects/362883_baseline/baseline/repository").getCanonicalFile();
    }

    private Verifier getVerifier(String project, File baselineRepo) throws Exception {
        Verifier verifier = getVerifier("/362883_baseline/" + project, false);
        verifier.getCliOptions().add("-De342-repo=" + ResourceUtil.P2Repositories.ECLIPSE_342.toString());
        verifier.getCliOptions().add("-Dbaseline-repo=" + baselineRepo.toURI().toString());
        return verifier;
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
    public void testContentChangedStrict() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");
    }

    @Test
    public void testContentChangedNonStrict() throws Exception {
        Verifier verifier = getVerifier("contentchanged", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline.strict=false");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");
    }

    @Test
    public void testNewAttachedArtifactStrict() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");
    }

    @Test
    public void testNewAttachedArtifactNonStrict() throws Exception {
        Verifier verifier = getVerifier("newattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline.strict=false");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");

        File basedir = new File(verifier.getBasedir());

        assertFileDoesNotExist(basedir, "feature02/target/baseline.feature02_root-1.0.0.1-root.zip");
        assertFileDoesNotExist(basedir, "repository/target/repository/binary/baseline.feature02_root_1.0.0.1");

        // TODO ideally, verify artifacts are detached from the project and are not installed/deployed
    }

    @Test
    public void testChangedAttachedArtifactStrict() throws Exception {
        Verifier verifier = getVerifier("changedattachedartifact", baselineRepo);

        try {
            verifier.executeGoals(Arrays.asList("clean", "package"));
        } catch (VerificationException expected) {
            //
        }
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");
    }

    @Test
    public void testChangedAttachedArtifactNonStrict() throws Exception {
        Verifier verifier = getVerifier("changedattachedartifact", baselineRepo);

        verifier.getCliOptions().add("-Dtycho.baseline.strict=false");

        verifier.executeGoals(Arrays.asList("clean", "package"));
        verifier.verifyTextInLog("baseline and reactor have same version but different contents");

        File repository = new File(verifier.getBasedir(), "repository/target/repository");
        assertBaselineContents(repository, "plugins/baseline.bundle01.source_1.0.0.1.jar");
    }

    private void assertBaselineContents(File repository, String path) throws IOException {
        Assert.assertTrue(isBaselineContents(repository, path));
    }

    private boolean isBaselineContents(File repository, String path) throws IOException {
        File file = new File(repository, path);
        File baselineFile = new File(baselineRepo, path);
        return FileUtils.contentEquals(baselineFile, file);
    }
}
