/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to changed API
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.tycho.core.ee.TargetDefinitionFile;
import org.eclipse.tycho.core.ee.TargetDefinitionFile.IULocation;
import org.eclipse.tycho.core.ee.TargetDefinitionFile.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;

public class TargetDefinitionUtil {

    /**
     * Resolves relative URLs in the given target definition file, with the specified resource as
     * base URL.
     * 
     * @param targetDefinitionFile
     *            The target definition file in which relative URLs shall be replaced.
     * @param base
     * @throws IOException
     */
    public static void makeURLsAbsolute(File targetDefinitionFile, File relocationBasedir) throws IOException {
        TargetDefinitionFile platform = TargetDefinitionFile.read(targetDefinitionFile);
        List<? extends TargetDefinition.Location> locations = platform.getLocations();
        for (TargetDefinition.Location location : locations) {
            List<Repository> repositories = ((IULocation) location).getRepositoryImpls();
            for (Repository repository : repositories) {
                makeRepositoryElementAbsolute(repository, relocationBasedir);
            }
        }
        TargetDefinitionFile.write(platform, targetDefinitionFile);
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
