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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * &quot;Unfreeze&quot; the build qualifier by changing the fourth version digit to
 * <tt>.qualifier</tt> in MANIFEST.MF and feature.xml and <tt>-SNAPSHOT</tt> in pom.xml,
 * respectively. Only versions of projects with packaging type <tt>eclipse-plugin</tt>,
 * <tt>eclipse-test-plugin</tt> or <tt>eclipse-feature</tt> which have a 4-digit OSGi version will
 * be changed.
 * 
 * This goal can be used after a release was done using {@link FreezeQualifierMojo} to start
 * development of the next version.
 */
@Mojo(name = "unfreeze-qualifier", requiresDirectInvocation = true)
public class UnFreezeQualifierMojo extends AbstractQualifierMojo {

    private static final Pattern FOUR_DIGIT_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.[^\\.]+");
    private static final Pattern THREE_DIGIT_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    protected Pattern getOldOsgiVersionPattern() {
        return FOUR_DIGIT_PATTERN;
    }

    protected Pattern getOldMavenVersionPattern() {
        return FOUR_DIGIT_PATTERN;
    }

    protected String getNewOsgiVersionQualifier() throws MojoFailureException {
        return "qualifier";
    }

    @Override
    protected String getNewMavenVersionQualifier() throws MojoFailureException {
        return "SNAPSHOT";
    }

    protected String createNewOsgiVersion(String oldOsgiVersion, String newQualifier) throws MojoExecutionException {
        Matcher matcher = THREE_DIGIT_PATTERN.matcher(oldOsgiVersion);
        matcher.find();
        return matcher.group() + "." + newQualifier;
    }

    protected String createNewPomVersion(String oldPomVersion, String newQualifier) {
        Matcher matcher = THREE_DIGIT_PATTERN.matcher(oldPomVersion);
        matcher.find();
        return matcher.group() + "-" + newQualifier;
    }

}
