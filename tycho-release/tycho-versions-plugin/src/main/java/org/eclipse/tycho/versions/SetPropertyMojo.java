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

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.tycho.versions.engine.VersionsEngine;

/**
 * <p>
 * Updates some properties in a pom file
 * </p>
 * 
 */
@Mojo(name = "set-property", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class SetPropertyMojo extends AbstractChangeMojo {

    /**
     * <p>
     * Comma separated list of names of POM properties to set, the new value will be the name of the
     * property prefixed with <code>new</code> and the first character changed to uppercase. For
     * example if <code>properties=myProperty</code> then there must be a user property specified
     * <code>newMyProperty=thevalue</code>.
     * </p>
     */
    @Parameter(property = "properties")
    private String properties;

    @Override
    protected void addChanges(List<String> artifacts, VersionsEngine engine)
            throws MojoExecutionException, IOException {
        if (properties == null || properties.isEmpty()) {
            throw new MojoExecutionException("Missing required parameter properties");
        }
        for (String artifactId : artifacts) {
            for (String propertyName : split(properties)) {
                if (propertyName.isBlank()) {
                    continue;
                }
                String userPropertyName = "new" + Character.toUpperCase(propertyName.charAt(0))
                        + propertyName.substring(1);
                String property = session.getUserProperties().getProperty(userPropertyName);
                if (property == null) {
                    throw new MojoExecutionException("No user property '" + userPropertyName + "' defined");
                }
                engine.addPropertyChange(artifactId, propertyName, property);
            }
        }
    }

}
