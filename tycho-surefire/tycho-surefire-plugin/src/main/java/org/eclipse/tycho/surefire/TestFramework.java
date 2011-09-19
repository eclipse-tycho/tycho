/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.classpath.ClasspathEntry;
import org.osgi.framework.Version;

public class TestFramework {

    public static final String TEST_JUNIT = "org.junit";

    public static final String TEST_JUNIT4 = "org.junit4";

    public String getTestFramework(List<ClasspathEntry> classpath) throws MojoExecutionException {
        String result = null;
        for (ClasspathEntry entry : classpath) {
            ArtifactKey key = entry.getArtifactKey();
            String testFramework = getTestFramework(key.getId(), key.getVersion(), result);
            if (testFramework != null) {
                result = testFramework;
            }
        }
        if (result != null) {
            return result;
        }
        return null;
    }

    private static String getTestFramework(String symbolicName, String versionStr, String currentTestFramework) {
        if (TestFramework.TEST_JUNIT.equals(symbolicName)) {
            Version version = Version.parseVersion(versionStr);
            if (version.getMajor() < 4) {
                // junit4 has precedence over junit3 if both are present
                if (!TestFramework.TEST_JUNIT4.equals(currentTestFramework)) {
                    return TestFramework.TEST_JUNIT;
                }
            } else {
                return TestFramework.TEST_JUNIT4;
            }
        } else if (TestFramework.TEST_JUNIT4.equals(symbolicName)) {
            return TestFramework.TEST_JUNIT4;
        }
        return null;
    }

}
