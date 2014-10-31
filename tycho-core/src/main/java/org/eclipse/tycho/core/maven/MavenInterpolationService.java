/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.maven;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.SingleResponseValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.eclipse.tycho.core.shared.InterpolationService;

/**
 * An {@link InterpolationService} that uses the plexus {@link StringSearchInterpolator} to
 * interpolate strings.
 * <p>
 * Value Sources are:
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
 * </p>
 *
 */
@Component(role = InterpolationService.class)
public class MavenInterpolationService implements InterpolationService {

    @Requirement
    private LegacySupport legacySupport;

    @Override
    public String interpolate(String value) {

        MavenSession mavenSession = legacySupport.getSession();
        final MavenProject mavenProject = mavenSession.getCurrentProject();

        final Properties baseProps = new Properties();
        baseProps.putAll(mavenProject.getProperties());
        baseProps.putAll(mavenSession.getSystemProperties());
        baseProps.putAll(mavenSession.getUserProperties());

        Settings settings = mavenSession.getSettings();

        StringSearchInterpolator interpolator = new StringSearchInterpolator();
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
        try {
            return interpolator.interpolate(value);
        } catch (InterpolationException e) {
            throw new RuntimeException(e);
        }
    }
}
