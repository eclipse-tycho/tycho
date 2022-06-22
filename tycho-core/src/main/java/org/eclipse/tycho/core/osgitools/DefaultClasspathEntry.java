/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.classpath.ClasspathEntry;

public class DefaultClasspathEntry implements ClasspathEntry {
    private final ReactorProject project;

    private final ArtifactKey key;

    private final List<File> locations;

    private final Collection<AccessRule> rules;

    public static class DefaultAccessRule implements AccessRule {
        private final String pattern;
        private final boolean discouraged;

        public DefaultAccessRule(String path, boolean discouraged) {
            if (path == null) {
                throw new NullPointerException();
            }

            this.pattern = path;
            this.discouraged = discouraged;
        }

        @Override
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (discouraged ? 1231 : 1237);
            result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
            return result;
        }

        @Override
        public String getPattern() {
            return pattern;
        }

        @Override
        public String toString() {
            return getPattern();
        }

        @Override
        public boolean isDiscouraged() {
            return discouraged;
        }

    }

    public DefaultClasspathEntry(ReactorProject project, ArtifactKey key, List<File> locations,
            Collection<AccessRule> rules) {
        this.key = Objects.requireNonNull(key);
        this.project = project;
        this.locations = locations;
        this.rules = rules;
    }

    @Override
    public List<File> getLocations() {
        return locations;
    }

    @Override
    public Collection<AccessRule> getAccessRules() {
        return rules;
    }

    @Override
    public ArtifactKey getArtifactKey() {
        return key;
    }

    @Override
    public ReactorProject getMavenProject() {
        return project;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DefaultClasspathEntry [key=");
        builder.append(key);
        builder.append("[" + key.getClass().getSimpleName() + "], ");
        if (project != null) {
            builder.append("project=");
            builder.append(project.getId());
            builder.append(", ");
        }
        if (locations != null) {
            builder.append("locations=");
            builder.append(locations);
            builder.append(", ");
        }
        if (rules != null) {
            builder.append("rules=");
            builder.append(rules);
        }
        builder.append("]");
        return builder.toString();
    }
}
