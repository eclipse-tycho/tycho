/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.packaging;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.tycho.core.facade.BuildProperties;

public class IncludeValidationHelper {

    private IncludeValidationHelper() {
    }

    public static void checkBinIncludesExist(MavenProject project, BuildProperties buildProperties,
            String... ignoredIncludes) throws MojoExecutionException {
        checkIncludesExist("bin.includes", buildProperties.getBinIncludes(), project, ignoredIncludes);
    }

    public static void checkSourceIncludesExist(MavenProject project, BuildProperties buildProperties)
            throws MojoExecutionException {
        checkIncludesExist("src.includes", buildProperties.getSourceIncludes(), project);
    }

    private static void checkIncludesExist(String buildPropertiesKey, List<String> includePatterns,
            MavenProject project, String... ignoredIncludes) throws MojoExecutionException {
        File baseDir = project.getBasedir();
        List<String> nonMatchingIncludes = new ArrayList<String>();
        List<String> ignoreList = Arrays.asList(ignoredIncludes);
        for (String includePattern : includePatterns) {
            if (ignoreList.contains(includePattern)) {
                continue;
            }
            if (new File(baseDir, includePattern).exists()) {
                continue;
            }
            // it does not exist as a file nor dir. Try if it matches any files as ant pattern 
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[] { includePattern });
            scanner.setBasedir(baseDir);
            scanner.scan();
            if (scanner.getIncludedFiles().length == 0) {
                nonMatchingIncludes.add(includePattern);
            }
        }
        if (nonMatchingIncludes.size() > 0) {
            throw new MojoExecutionException(new File(baseDir, "build.properties").getAbsolutePath() + ": "
                    + buildPropertiesKey + " value(s) " + nonMatchingIncludes + " do not match any files.");
        }
    }

}
