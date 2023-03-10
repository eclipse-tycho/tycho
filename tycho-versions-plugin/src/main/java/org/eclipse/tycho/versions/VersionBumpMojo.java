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
package org.eclipse.tycho.versions;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.helper.ProjectHelper;

/**
 * This mojo allows configuration of an automatic version bump behavior in combination with the
 * <code>org.eclipse.tycho.extras:tycho-p2-extras-plugin:compare-version-with-baselines</code> goal
 * or similar. It works the following way:
 * <ul>
 * <li>You can either configure this in the pom (e.g. in a profile) with an explicit execution, or
 * specify it on the command line like <code>mvn [other goals and options]
 * org.eclipse.tycho:tycho-versions-plugin:bump-versions</li>
 * <li>if the build fails with a VersionBumpRequiredException the projects version is
 * incremented</li>
 * <li>one can now run the build again with the incremented version and verify the automatic applied
 * changes</li>
 * </ul>
 * 
 */
@Mojo(name = VersionBumpMojo.NAME, threadSafe = true, defaultPhase = LifecyclePhase.VERIFY, requiresProject = true)
public class VersionBumpMojo extends AbstractMojo {

    static final String ARTIFACT_ID = "tycho-versions-plugin";

    static final String GROUP_ID = "org.eclipse.tycho";

    static final int DEFAULT_INCREMENT = 1;

    static final String NAME = "bump-versions";

    static final String PROPERTY_INCREMENT = "tycho." + NAME + ".increment";

    /**
     * Configures the default increment of micro version to use if no version is recommended by the
     * VersionBumpRequiredException produced by the version check plugin.
     */
    @Parameter(property = PROPERTY_INCREMENT, defaultValue = DEFAULT_INCREMENT + "")
    private int increment = DEFAULT_INCREMENT;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //actual work is performed in VersionBumpBuildListener
    }

    public static int getIncrement(MavenSession mavenSession, MavenProject project, ProjectHelper projectHelper) {
        String prop = mavenSession.getUserProperties().getProperty(PROPERTY_INCREMENT);
        if (prop != null) {
            return Integer.parseInt(prop);
        }
        Xpp3Dom configuration = projectHelper.getPluginConfiguration(GROUP_ID, ARTIFACT_ID, NAME, project,
                mavenSession);
        if (configuration != null) {
            Xpp3Dom child = configuration.getChild("increment");
            if (child != null) {
                return Integer.parseInt(child.getValue());
            }
        }
        return DEFAULT_INCREMENT;
    }

}
