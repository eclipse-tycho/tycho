package org.eclipse.tycho.core.test;

import java.io.File;
import java.util.Properties;

import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class DefaultTargetPlatformConfigurationReaderTest extends AbstractTychoMojoTestCase {

    public void test_optionalDependencies_and_targetFile_cannot_be_used_together() throws Exception {
        File basedir = getBasedir("projects/optionalDependencies_and_targetFile");

        Properties properties = new Properties();
        properties.put("tycho-version", TychoVersion.getTychoVersion());

        try {
            getSortedProjects(basedir, properties, null);
            fail("<optionalDependencies> and <target> target platform configuration parameters cannot be used together");
        } catch (RuntimeException e) {
            // TODO validate exception
            e.printStackTrace();
        }

    }
}
