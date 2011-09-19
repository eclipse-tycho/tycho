package org.eclipse.tycho.test.util;

import java.io.File;

public class P2RepositoryTool {

    private final File repoLocation;

    private P2RepositoryTool(File repoLocation) {
        this.repoLocation = repoLocation;
    }

    public static P2RepositoryTool forEclipseRepositoryModule(File projectRootFolder) {
        File repoLocation = new File(projectRootFolder, "target/repository");
        if (!repoLocation.isDirectory()) {
            throw new IllegalStateException("Not an eclipse-repository project, or project has not been built: "
                    + projectRootFolder);
        }
        return new P2RepositoryTool(repoLocation);
    }

    public File getBundleArtifact(String bundleId, String version) {
        String pathInRepo = "plugins/" + bundleId + "_" + version + ".jar";
        return new File(repoLocation, pathInRepo);
    }
}
