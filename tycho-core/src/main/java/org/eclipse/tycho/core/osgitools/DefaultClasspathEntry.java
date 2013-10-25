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

        public String getPattern() {
            return pattern;
        }

        @Override
        public String toString() {
            return getPattern();
        }

        public boolean isDiscouraged() {
            return discouraged;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (discouraged ? 1231 : 1237);
            result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DefaultAccessRule other = (DefaultAccessRule) obj;
            if (discouraged != other.discouraged)
                return false;
            if (pattern == null) {
                if (other.pattern != null)
                    return false;
            } else if (!pattern.equals(other.pattern))
                return false;
            return true;
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
