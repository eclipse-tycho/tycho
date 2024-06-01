/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.versions.engine.VersionsEngine;

/**
 * <p>
 * Sets the version of the current project and child projects with the same version, and updates
 * references as necessary.
 * </p>
 * <p>
 * The set-version goal implements a version refactoring for a Tycho reactor: When updating the
 * version of a project, it consistently updates the version strings in the project's configuration
 * files (e.g. pom.xml and META-INF/MANIFEST.MF) and all references to that project (e.g. in a
 * feature.xml).
 * </p>
 * <p>
 * In many cases, the set-version goal changes the version of multiple projects or entities at once.
 * In addition to the current project, child projects with the same version are also changed. The
 * set of version changes is determined according to the following rules:
 * </p>
 * <ul>
 * <li>When the parent project of a project is changed and the project has the same version as the
 * parent project, the project is also changed.</li>
 * <li>When an <code>eclipse-plugin</code> project is changed and the plugin exports a package with a
 * version which is the same as the unqualified project version, the version of the package is also
 * changed.
 * <li>Require-Bundle and Fragment-Host Version Range in references to an <code>eclipse-plugin</code>
 * that changed version will be updated:
 * <ul>
 * <li>if the newVersion becomes out of the original VersionRange
 * <li>or if {@link #updateVersionRangeMatchingBounds} is true and one of the bounds is matching the
 * original version
 * </ul>
 * <li>When an <code>eclipse-repository</code> project is changed and a product file in the project has
 * an equivalent version, the version in the product file is also changed.</li>
 * </ul>
 * 
 */
@Mojo(name = "set-version", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class SetVersionMojo extends AbstractChangeMojo {
    /**
     * <p>
     * The new version to set to the current project and other entities which have the same version
     * as the current project.
     * </p>
     */
    @Parameter(property = "newVersion", required = true, alias = "developmentVersion")
    private String newVersion;

    /**
     * <p>
     * When true bounds of OSGI version ranges referencing the version of an element that changed
     * version will be updated to match the newVersion.
     * </p>
     */
    @Parameter(property = "updateVersionRangeMatchingBounds", defaultValue = "false")
    private boolean updateVersionRangeMatchingBounds;

    /**
     * <p>
     * Comma separated list of names of POM properties to set the new version to. Note that
     * properties are only changed in the projects explicitly listed by the {@link #artifacts}
     * parameter.
     * </p>
     * 
     * @since 0.18.0
     */
    @Parameter(property = "properties")
    private String properties;

    @Override
    protected void addChanges(List<String> artifacts, VersionsEngine engine)
            throws MojoExecutionException, IOException {
        engine.setUpdateVersionRangeMatchingBounds(updateVersionRangeMatchingBounds);
        // initial changes
        for (String artifactId : artifacts) {
            engine.addVersionChange(artifactId, newVersion);
            for (String propertyName : split(properties)) {
                engine.addPropertyChange(artifactId, propertyName, newVersion);
            }
        }
    }

}
