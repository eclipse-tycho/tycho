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
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.List;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;

public class DefaultClasspathEntry implements ClasspathEntry {
    private final ReactorProject project;

    private final ArtifactKey key;

    private final List<File> locations;

    private final List<AccessRule> rules;

    public static class DefaultAccessRule implements AccessRule {
        private final String pattern;

        private final boolean discouraged;

        public DefaultAccessRule(String path, boolean discouraged) {
            this.pattern = path;
            this.discouraged = discouraged;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AccessRule)) {
                return false;
            }
            AccessRule other = (AccessRule) obj;
            return isDiscouraged() == other.isDiscouraged() && getPattern().equals(other.getPattern());
        }

        public String getPattern() {
            return pattern;
        }

        public boolean isDiscouraged() {
            return discouraged;
        }

    }

    public DefaultClasspathEntry(ReactorProject project, ArtifactKey key, List<File> locations, List<AccessRule> rules) {
        this.project = project;
        this.key = key;
        this.locations = locations;
        this.rules = rules;
    }

    public List<File> getLocations() {
        return locations;
    }

    public List<AccessRule> getAccessRules() {
        return rules;
    }

    public ArtifactKey getArtifactKey() {
        return key;
    }

    public ReactorProject getMavenProject() {
        return project;
    }
}
