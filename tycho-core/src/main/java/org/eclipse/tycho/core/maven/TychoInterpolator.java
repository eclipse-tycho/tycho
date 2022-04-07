/*******************************************************************************
 * Copyright (c) 2014, 2015 Bachmann electronic and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bachmann electronic - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.SingleResponseValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.eclipse.tycho.Interpolator;

/**
 * Class thats interpolates string values like ${project.artifactId}. It is using the
 * {@link StringSearchInterpolator} and hiding all plexus interfaces/classes.
 * <p>
 * Value sources this interpolator uses:
 * <ul>
 * <li>{@link MavenSession#getSystemProperties()}</li>
 * <li>{@link MavenSession#getUserProperties()}</li>
 * <li>{@link MavenProject#getProperties()}</li>
 * <li>{@link MavenProject} as {@link PrefixedObjectValueSource}</li>
 * <li>{@link MavenSession#getSettings()} as {@link PrefixedObjectValueSource}</li>
 * <li>{@link MavenProject#getBasedir()} for ${basedir}</li>
 * <li>{@link Settings#getLocalRepository()} for ${localRepository}</li>
 * </ul>
 * </p>
 *
 */
public class TychoInterpolator implements Interpolator {

    private StringSearchInterpolator interpolator;

    public TychoInterpolator(MavenSession mavenSession, MavenProject mavenProject) {
        final Properties baseProps = new Properties();
        // The order how the properties been added is important! 
        // It defines which properties win over others 
        // (session user properties overwrite system properties overwrite project properties)
        baseProps.putAll(mavenProject.getProperties());
        baseProps.putAll(mavenSession.getSystemProperties());
        baseProps.putAll(mavenSession.getUserProperties());

        final Settings settings = mavenSession.getSettings();

        // roughly match resources plugin behaviour
        // Using the project and settings as object value source to get things replaces like
        // ${project.artifactId}...;
        // Simple string replacement for ${localRepository}, ${version}, ${basedir};
        // An and string replacement for all property values
        // (session user properties, system properties, project properties).

        interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new PrefixedObjectValueSource("project", mavenProject));
        interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        interpolator.addValueSource(new SingleResponseValueSource("localRepository", settings.getLocalRepository()));
        interpolator.addValueSource(new SingleResponseValueSource("version", mavenProject.getVersion()));
        interpolator.addValueSource(new SingleResponseValueSource("basedir", mavenProject.getBasedir()
                .getAbsolutePath()));
        interpolator.addValueSource(new ValueSource() {
            @Override
            public Object getValue(String expression) {
                return baseProps.getProperty(expression);
            }

            @Override
            public void clearFeedback() {
            }

            @Override
            @SuppressWarnings("rawtypes")
            public List getFeedback() {
                return Collections.EMPTY_LIST;
            }
        });
    }

    @Override
    public String interpolate(String input) {
        try {
            return interpolator.interpolate(input);
        } catch (org.codehaus.plexus.interpolation.InterpolationException e) {
            throw new RuntimeException("Error while interpolating value \"" + input + "\"", e);
        }
    }

}
