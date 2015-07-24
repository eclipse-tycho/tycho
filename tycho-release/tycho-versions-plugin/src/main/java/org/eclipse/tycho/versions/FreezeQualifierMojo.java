/*******************************************************************************
 * Copyright (c) 2016 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions;

import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.TychoProperties;

/**
 * Freeze the current build qualifier (as computed by tycho-packaging-plugin:build-qualifier) by
 * hardcoding it in MANIFEST.MF, feature.xml and pom.xml (unless pom-less). Only versions of
 * projects with packaging type <tt>eclipse-plugin</tt>, <tt>eclipse-test-plugin</tt> or
 * <tt>eclipse-feature</tt> which have a 4-digit OSGi version ending with literal
 * &quot;.qualifier&quot; will be changed. For a given artifact, its maven version (in pom.xml) and
 * its OSGi version (in MANIFEST.MF or feature.xml) will be hardcoded to exactly the same value.
 * 
 * Note that at least lifecycle phase &quot;validate&quot; must be run prior to executing this goal.
 * This ensures the current build qualifier is computed.<br/>
 * 
 * This goal can be useful as a preparation step before doing a release build to ensure a
 * reproducible build of released versions.
 *
 */
@Mojo(name = "freeze-qualifier", requiresDirectInvocation = true)
public class FreezeQualifierMojo extends AbstractQualifierMojo {

    private static final String QUALIFIER_SUFFIX = ".qualifier";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private static final Pattern ENDS_WITH_QUALIFIER_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.qualifier");
    private static final Pattern ENDS_WITH_SNAPSHOT_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+-SNAPSHOT");

    /**
     * Optional version suffix to be appended to the computed build qualifier value.<br/>
     * Examples: <tt>-M1</tt>, <tt>-RELEASE</tt>
     */
    @Parameter(property = "versionSuffix")
    private String versionSuffix;

    protected Pattern getOldOsgiVersionPattern() {
        return ENDS_WITH_QUALIFIER_PATTERN;
    }

    protected Pattern getOldMavenVersionPattern() {
        return ENDS_WITH_SNAPSHOT_PATTERN;
    }

    protected String getNewOsgiVersionQualifier() throws MojoFailureException {
        String qualifier = project.getProperties().getProperty(TychoProperties.BUILD_QUALIFIER);
        if (qualifier == null) {
            throw new MojoFailureException("project property ${" + TychoProperties.BUILD_QUALIFIER
                    + "} not set. At least lifecycle phase 'validate' must be executed before this goal");
        }
        if (versionSuffix != null) {
            qualifier = qualifier + versionSuffix;
        }
        return qualifier;
    }

    @Override
    protected String getNewMavenVersionQualifier() throws MojoFailureException {
        return getNewOsgiVersionQualifier();
    }

    protected String createNewOsgiVersion(String oldOsgiVersion, String newQualifier) throws MojoExecutionException {
        return oldOsgiVersion.substring(0, oldOsgiVersion.length() - QUALIFIER_SUFFIX.length()) + "." + newQualifier;
    }

    protected String createNewPomVersion(String oldPomVersion, String newQualifier) {
        return oldPomVersion.substring(0, oldPomVersion.length() - SNAPSHOT_SUFFIX.length()) + "." + newQualifier;
    }

}
