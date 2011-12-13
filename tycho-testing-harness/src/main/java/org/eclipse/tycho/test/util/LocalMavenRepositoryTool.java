/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;

public class LocalMavenRepositoryTool {

    private File localRepo;

    /**
     * Creates a default instance of the <code>LocalRepositoryUtils</code>, using the local Maven
     * repository location obtained from {@link EnvironmentUtil}.
     */
    public LocalMavenRepositoryTool() {
        localRepo = new File(EnvironmentUtil.getLocalRepo());
        if (!localRepo.isDirectory()) {
            throw new IllegalStateException("Local Maven repository does not exist: " + localRepo);
        }
    }

//    public File getArtifactFile(GAV gav, String classifier, String extension) {
//        return getArtifactFile(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, extension);
//    }

    public File getArtifactFile(String groupId, String artifactId, String version, String classifier, String extension) {
        String groupPath = groupId.replace('.', '/');
        String artifactPath = groupPath + '/' + artifactId + '/' + version;
        String artifactName = artifactId + "-" + version + (classifier == null ? "" : "-" + classifier) + "."
                + extension;
        return new File(localRepo, artifactPath + '/' + artifactName);
    }

    /**
     * Hides all artifacts built and installed by Tycho from future Tycho builds.
     * <p>
     * Background: Artifacts built and installed by Tycho are visible to all other Tycho builds in
     * order to allow re-builds of individual modules. This method allows to prevent this for future
     * builds.
     * 
     * @throws IOException
     *             if the list of locally built Tycho artifacts cannot be emptied.
     */
    public void hideAllLocalTychoArtifacts() throws IOException {
        File listOfTychoBuiltArtifact = new File(localRepo, ".meta/p2-local-metadata.properties");
        if (listOfTychoBuiltArtifact.exists()) {
            boolean success = listOfTychoBuiltArtifact.delete();
            if (!success)
                throw new IOException("Could not delete " + listOfTychoBuiltArtifact);
        }
    }

}
