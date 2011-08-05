/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
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
import java.net.URI;
import java.util.List;

import org.eclipse.tycho.p2.resolver.TargetDefinitionFile;
import org.eclipse.tycho.p2.resolver.TargetDefinitionFile.IULocation;
import org.eclipse.tycho.p2.resolver.TargetDefinitionFile.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;

public class TargetDefinitionUtil {

    public enum BaseLocation {
        /** Use the target file location as base */
        TARGET_FILE,
        /**
         * Use the location of the target file in the sources as base, i.e. the location before the
         * test project was copied to the target folder by the verifier.
         */
        TARGET_FILE_IN_SOURCES
    }

    private static final String TEST_PROJECT_SOURCE_PATH = File.separator + "projects" + File.separator;
    private static final String TEST_PROJECT_TARGET_PATH = File.separator + "target" + TEST_PROJECT_SOURCE_PATH;

    /**
     * Resolves relative URLs in the given target definition file, with the specified resource as
     * base URL.
     * 
     * @param targetDefinitionFile
     *            The target definition file in which relative URLs shall be replaced.
     * @param base
     * @throws IOException
     */
    public static void makeURLsAbsolute(File targetDefinitionFile, BaseLocation base) throws IOException {
        File logicalTargetFileLocation = getLogicalFileLocation(targetDefinitionFile,
                base == BaseLocation.TARGET_FILE_IN_SOURCES);

        TargetDefinitionFile platform = TargetDefinitionFile.read(targetDefinitionFile);
        List<? extends TargetDefinition.Location> locations = platform.getLocations();
        for (TargetDefinition.Location location : locations) {
            List<Repository> repositories = ((IULocation) location).getRepositoryImpls();
            for (Repository repository : repositories) {
                makeRepositoryElementAbsolute(repository, logicalTargetFileLocation);
            }
        }
        TargetDefinitionFile.write(platform, targetDefinitionFile);
    }

    private static File getLogicalFileLocation(File physicalLocation, boolean useSourceLocation) {
        if (!useSourceLocation) {
            return physicalLocation;
        }
        return getLocationInTestProjectSources(physicalLocation);
    }

    private static File getLocationInTestProjectSources(File targetLocation) {
        if (!targetLocation.exists())
            throw new IllegalArgumentException("Resource does not exist: " + targetLocation);

        String path = targetLocation.getAbsolutePath();
        if (!path.contains(TEST_PROJECT_TARGET_PATH))
            throw new IllegalArgumentException("Cannot determine souce location of " + path);
        File sourceLocation = new File(path.replace(TEST_PROJECT_TARGET_PATH, TEST_PROJECT_SOURCE_PATH));

        if (!sourceLocation.exists()) {
            throw new IllegalArgumentException("Source location " + sourceLocation + " of " + targetLocation
                    + " does not exist.");
        }
        return sourceLocation;
    }

    private static void makeRepositoryElementAbsolute(Repository repositoryElement, File fileLocation) {
        URI repositoryURL = repositoryElement.getLocation();
        URI absoluteRepositoryURL = fileLocation.toURI().resolve(repositoryURL);
        repositoryElement.setLocation(absoluteRepositoryURL.toString());
    }

    /**
     * Overwrites all repository URLs in the target file.
     */
    public static void setRepositoryURLs(File targetDefinitionFile, String url) throws IOException {
        TargetDefinitionFile platform = TargetDefinitionFile.read(targetDefinitionFile);
        for (TargetDefinition.Location location : platform.getLocations()) {
            for (Repository repository : ((IULocation) location).getRepositoryImpls()) {
                repository.setLocation(url);
            }
        }
        TargetDefinitionFile.write(platform, targetDefinitionFile);
    }

}
