/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;

/**
 * {@link RepositoryReader} that reads the artifact file locations from the
 * "local-artifacts.properties" file.
 * 
 * @see RepositoryLayoutHelper#FILE_NAME_LOCAL_ARTIFACTS
 */
class ModuleArtifactReader implements RepositoryReader {
    private File localArtifactsFile;

    private final Map<String, File> artifacts;

    public ModuleArtifactReader(File targetDirectory) throws ProvisionException {
        this.localArtifactsFile = new File(targetDirectory, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
        this.artifacts = readArtifactLocations(this.localArtifactsFile);
    }

    public InputStream getContents(String remoteRelpath) throws IOException {
        // can only return artifacts by classifier
        throw new UnsupportedOperationException();
    }

    public InputStream getContents(GAV gav, String classifier, String extension) throws IOException {
        // GAV parameter may only refer to current module; TODO verify this?

        File artifactFile = artifacts.get(classifier);
        if (artifactFile == null) {
            throw new IllegalStateException("Classifier " + classifier + " is missing in "
                    + localArtifactsFile.getAbsolutePath());
        }
        return new FileInputStream(artifactFile);
    }

    private static Map<String, File> readArtifactLocations(File mapFile) throws ProvisionException {
        try {
            Properties map = loadProperties(mapFile);

            Map<String, File> result = new HashMap<String, File>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                if (RepositoryLayoutHelper.KEY_ARTIFACT_MAIN.equals(key)) {
                    result.put(null, localArtifactEntryToFile(entry));
                } else if (key.startsWith(RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED)) {
                    String classifier = key.substring(RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED.length());
                    result.put(classifier, localArtifactEntryToFile(entry));
                }
            }
            return result;
        } catch (IOException e) {
            Status errorStatus = new Status(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ,
                    "I/O exception while reading " + mapFile, e);
            throw new ProvisionException(errorStatus);
        }
    }

    private static File localArtifactEntryToFile(Map.Entry<?, ?> entry) {
        // paths are absolute in a format suitable for the running OS
        return new File((String) entry.getValue());
    }

    private static Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        FileInputStream propertiesStream = new FileInputStream(propertiesFile);
        try {
            properties.load(propertiesStream);
        } finally {
            propertiesStream.close();
        }
        return properties;
    }
}
