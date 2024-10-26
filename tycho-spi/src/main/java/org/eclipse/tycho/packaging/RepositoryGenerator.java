/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.tycho.ArtifactType;
import org.slf4j.Logger;

/**
 * A {@link RepositoryGenerator} is responsible for generate a specific repository format from a set
 * of items.
 *
 */
public interface RepositoryGenerator {

    /**
     * Creates a repository from the supplied list of maven projects
     * 
     * @param projects
     *            the list of projects to use
     * @param configuration
     *            the configuration for the resulting repository
     * @return the generated repository
     * @throws IOException
     * @throws MojoExecutionException
     *             if an internal error occurs while generating the repository
     * @throws MojoFailureException
     *             if a user configuration error occurs
     */
    File createRepository(List<MavenProject> projects, RepositoryConfiguration configuration)
            throws IOException, MojoExecutionException, MojoFailureException;

    /**
     * Determines if a given project is interesting for this generator, a generator might be capable
     * of processing specific things and should probably be able to generate some content from such
     * a project, the default implementation includes <code>"jar"</code>, <code>"bundle"</code>,
     * {@value ArtifactType#TYPE_ECLIPSE_PLUGIN} and {@value ArtifactType#TYPE_ECLIPSE_TEST_PLUGIN}
     * packaged projects as potentially interesting.
     * 
     * @param mavenProject
     * @return <code>true</code> if the project is interesting or <code>false</code> otherwise.
     */
    default boolean isInteresting(MavenProject mavenProject) {
        String packaging = mavenProject.getPackaging();
        return "jar".equalsIgnoreCase(packaging) || "bundle".equalsIgnoreCase(packaging)
                || ArtifactType.TYPE_ECLIPSE_PLUGIN.equals(packaging)
                || ArtifactType.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging);
    }

    static enum RepositoryLayout {
        /**
         * reference artifacts using mvn: protocol
         */
        maven,
        /**
         * references artifacts as local files and copy them into a folder
         */
        local;
    }

    interface RepositoryConfiguration {

        /**
         * @return he base folder where the repository should be generated
         */
        File getDestination();

        /**
         * @return the desired repository layout, if the generator do not understand the layout it
         *         should throw a {@link MojoFailureException}
         */
        RepositoryLayout getLayout();

        /**
         * @return the user visible name of the repository. This is a hint, if the provider does not
         *         support such thing it could ignore this
         */
        String getRepositoryName();

        /**
         * @return the additional respository configuration.
         */
        PlexusConfiguration getConfiguration();

        /**
         * @return the logger to issue logging messages
         */
        Logger getLogger();
    }
}
