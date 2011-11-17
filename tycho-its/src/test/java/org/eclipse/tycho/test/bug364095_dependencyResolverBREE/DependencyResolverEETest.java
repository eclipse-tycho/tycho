package org.eclipse.tycho.test.bug364095_dependencyResolverBREE;

import java.util.Arrays;

import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.eclipse.tycho.test.util.ResourceUtil;
import org.junit.Test;

public class DependencyResolverEETest extends AbstractTychoIntegrationTest {

    @Test
    public void eeFromBREE() throws Exception {
        Verifier verifier = getVerifier("/364095_dependencyResolverBREE/ee-from-bree", false);
        verifier.getCliOptions().add(
                "-Djavax.xml-repo=" + ResourceUtil.resolveTestResource("repositories/javax.xml").toURI().toString());
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void eeFromPOM() throws Exception {
        Verifier verifier = getVerifier("/364095_dependencyResolverBREE/ee-from-pom", false);
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void eeFromJRE() throws Exception {
        Verifier verifier = getVerifier("/364095_dependencyResolverBREE/ee-from-jre", false);
        verifier.executeGoals(Arrays.asList("clean", "install"));
        verifier.verifyErrorFreeLog();
    }
}
