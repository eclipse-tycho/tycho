/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronic and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.SingleResponseValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * Factory for creating an {@link Interpolator} that can be used to replaces variables like
 * ${basedir} in strings.
 * 
 */
@Component(role = InterpolatorFactory.class)
public class InterpolatorFactory {

    /**
     * Creates a new {@link Interpolator} using the given {@link MavenSession} and
     * {@link MavenProject}.
     * 
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
     * 
     * @param mavenSession
     *            the MavenSession (must not be null)
     * @param mavenProject
     *            the MavenProject (must not be null)
     * @return returns the Interpolator
     */
    public Interpolator createInterpolator(MavenSession mavenSession, MavenProject mavenProject) {
        final Properties baseProps = new Properties();
        baseProps.putAll(mavenProject.getProperties());
        baseProps.putAll(mavenSession.getSystemProperties());
        baseProps.putAll(mavenSession.getUserProperties());

        final Settings settings = mavenSession.getSettings();

        // roughly match resources plugin behaviour

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new PrefixedObjectValueSource("project", mavenProject));
        interpolator.addValueSource(new PrefixedObjectValueSource("settings", settings));
        interpolator.addValueSource(new SingleResponseValueSource("localRepository", settings.getLocalRepository()));
        interpolator.addValueSource(new SingleResponseValueSource("basedir", mavenProject.getBasedir()
                .getAbsolutePath()));
        interpolator.addValueSource(new ValueSource() {
            public Object getValue(String expression) {
                return baseProps.getProperty(expression);
            }

            public void clearFeedback() {
            }

            @SuppressWarnings("rawtypes")
            public List getFeedback() {
                return Collections.EMPTY_LIST;
            }
        });
        return interpolator;
    }

}
