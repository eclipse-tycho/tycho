/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.maven.repository;

import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED;
import static org.eclipse.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_MAIN;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.tycho.p2.repository.AbstractRepositoryReader;
import org.eclipse.tycho.p2.repository.GAV;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.repository.RepositoryReader;

/**
 * {@link RepositoryReader} that reads the artifact file locations from the
 * "local-artifacts.properties" file.
 * 
 * @see RepositoryLayoutHelper#FILE_NAME_LOCAL_ARTIFACTS
 */
// TODO 348586 rename? this now also writes
class ModuleArtifactReader extends AbstractRepositoryReader {

    private File mapFile;
    private final Map<String, File> artifacts = new LinkedHashMap<String, File>();

    private File automaticArtifactFolder;

    // BEGIN construction

    public static ModuleArtifactReader restoreInstance(File location) throws ProvisionException {
        ModuleArtifactReader instance = new ModuleArtifactReader(location);

        instance.load();
        return instance;
    }

    public static ModuleArtifactReader createInstance(File repositoryRoot) throws ProvisionException {
        return new ModuleArtifactReader(repositoryRoot);
    }

    private ModuleArtifactReader(File repositoryRoot) {
        // TODO constant FILE_NAME_LOCAL_ARTIFACTS should only be needed here 
        this.mapFile = new File(repositoryRoot, RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS);
        this.automaticArtifactFolder = new File(repositoryRoot, "extraArtifacts");
    }

    public File getLocalArtifactLocation(GAV gav, String classifier, String extension) {
        // GAV parameter may only refer to current module; TODO verify this?

        File artifactFile = artifacts.get(classifier);
        if (artifactFile == null) {
            throw new IllegalStateException("Classifier " + classifier + " is missing in " + mapFile.getAbsolutePath());
        }
        return artifactFile;
    }

    public boolean contains(String classifier) {
        return artifacts.containsKey(classifier);
    }

    public void add(String classifier, File fileLocation) throws ProvisionException {
        if (fileLocation == null)
            throw new NullPointerException();

        File previousValue = artifacts.put(classifier, fileLocation);
        if (previousValue != null) {
            throw new IllegalStateException("Classifier " + classifier + " already exists in " + mapFile);
        }
        store();
    }

    public File addToAutomaticLocation(String classifier) throws ProvisionException {
        automaticArtifactFolder.mkdirs();
        File newFileLocation = new File(automaticArtifactFolder, classifier + ".zip");

        add(classifier, newFileLocation);

        return newFileLocation;
    }

    private void load() throws ProvisionException {
        artifacts.clear();

        try {
            Properties map = loadProperties(mapFile);

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                if (RepositoryLayoutHelper.KEY_ARTIFACT_MAIN.equals(key)) {
                    artifacts.put(null, localArtifactEntryToFile(entry));
                } else if (key.startsWith(RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED)) {
                    String classifier = key.substring(RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED.length());
                    artifacts.put(classifier, localArtifactEntryToFile(entry));
                }
            }
        } catch (IOException e) {
            String message = "I/O error while reading repository from " + mapFile;
            int code = ProvisionException.REPOSITORY_FAILED_READ;
            Status status = new Status(IStatus.ERROR, Activator.ID, code, message, e);
            throw new ProvisionException(status);
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

    private void store() throws ProvisionException {
        Properties outputProperties = new Properties();

        for (Entry<String, File> entry : artifacts.entrySet()) {
            if (entry.getKey() == null) {
                outputProperties.put(KEY_ARTIFACT_MAIN, entry.getValue().getAbsolutePath());
            } else {
                outputProperties.put(KEY_ARTIFACT_ATTACHED + entry.getKey(), entry.getValue().getAbsolutePath());
            }
        }

        try {
            writeProperties(outputProperties, mapFile);
        } catch (IOException e) {
            String message = "I/O error while writing repository to " + mapFile;
            int code = ProvisionException.REPOSITORY_FAILED_WRITE;
            Status status = new Status(IStatus.ERROR, Activator.ID, code, message, e);
            throw new ProvisionException(status);
        }

    }

    private static void writeProperties(Properties properties, File outputFile) throws IOException {
        FileOutputStream outputStream;
        outputStream = new FileOutputStream(outputFile);

        try {
            properties.store(outputStream, null);
        } finally {
            outputStream.close();
        }
    }
}
