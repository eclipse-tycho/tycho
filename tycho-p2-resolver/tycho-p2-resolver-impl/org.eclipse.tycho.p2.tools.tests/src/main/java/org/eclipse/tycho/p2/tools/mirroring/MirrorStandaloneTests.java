package org.eclipse.tycho.p2.tools.mirroring;

import static org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest.repoFile;
import static org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest.sourceRepos;
import static org.junit.Assert.assertTrue;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.mirroring.facade.MirrorApplicationService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MirrorStandaloneTests {
    private static final int DEFAULT_FLAGS = MirrorApplicationService.MIRROR_ARTIFACTS;
    private static final String DEFAULT_NAME = "dummy";

    private DestinationRepositoryDescriptor destinationRepo;
    private MavenLogger logger;

    private MirrorApplicationServiceImpl subject;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void initTestContext() {
        logger = new MirrorApplicationServiceTest.MemoryLog();
        destinationRepo = new DestinationRepositoryDescriptor(tempFolder.newFolder("dest"), DEFAULT_NAME);

        subject = new MirrorApplicationServiceImpl();
    }

    @Test
    public void testMirrorAllUnits() throws Exception {
        subject.mirrorStandalone(sourceRepos("e342", "fragments"), destinationRepo, DEFAULT_FLAGS,
                tempFolder.getRoot(), logger);

        assertTrue(repoFile(destinationRepo, "plugins/org.eclipse.core.runtime_3.4.0.v20080512.jar").exists());
        assertTrue(repoFile(destinationRepo,
                "plugins/org.eclipse.equinox.launcher.gtk.linux.x86_64_1.0.101.R34x_v20080731.jar").exists());
        assertTrue(repoFile(destinationRepo,
                "plugins/org.eclipse.equinox.launcher.win32.win32.x86_1.0.101.R34x_v20080731.jar").exists());
    }

}
