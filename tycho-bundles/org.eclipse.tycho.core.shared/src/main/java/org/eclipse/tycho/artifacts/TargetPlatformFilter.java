/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.artifacts;

import java.util.Objects;
import java.util.StringJoiner;

public final class TargetPlatformFilter {

    private final CapabilityPattern scopePattern;
    private final FilterAction action;
    private final CapabilityPattern actionPattern;

    public static TargetPlatformFilter removeAllFilter(CapabilityPattern scopePattern) {
        return new TargetPlatformFilter(scopePattern, FilterAction.REMOVE_ALL, null);
    }

    public static TargetPlatformFilter restrictionFilter(CapabilityPattern scopePattern,
            CapabilityPattern restrictionPattern) {
        return new TargetPlatformFilter(scopePattern, FilterAction.RESTRICT, restrictionPattern);
    }

    private TargetPlatformFilter(CapabilityPattern scopePattern, FilterAction action, CapabilityPattern actionPattern) {
        this.scopePattern = scopePattern;
        this.action = action;
        this.actionPattern = actionPattern;
    }

    /**
     * Returns the pattern to determine the scope of the filter, i.e. the set of units to be
     * processed by the filter.
     */
    public CapabilityPattern getScopePattern() {
        return scopePattern;
    }

    /**
     * Returns the action to be performed.
     */
    public FilterAction getAction() {
        return action;
    }

    /**
     * Returns the pattern for the {@link FilterAction#RESTRICT} action. All units in scope which do
     * not match this pattern shall be removed.
     * 
     * <p>
     * If the action pattern returns <code>null</code> for its type, or type and id attributes,
     * these values shall be inherited from the scope pattern. This ensures that filters with a
     * short <tt>&lt;restrictTo&gt;</tt> pattern with only a single <tt>&lt;version&gt;</tt> or
     * <tt>&lt;versionRange&gt;</tt> attribute work as expected.
     * </p>
     */
    public CapabilityPattern getActionPattern() {
        return actionPattern;
    }

    @Override
    public String toString() {
        String commonPart = "TargetPlatformFilter(scope=" + scopePattern + ", action=" + action;
        return switch (action) {
        case REMOVE_ALL -> commonPart + ")";
        case RESTRICT -> commonPart + ", restriction=" + actionPattern + ")";
        default -> super.toString();
        };
    }

    public enum FilterAction {
        REMOVE_ALL, RESTRICT
    }

    // TODO share throughout Tycho?
    public enum CapabilityType {
        OSGI_BUNDLE, JAVA_PACKAGE, P2_INSTALLABLE_UNIT;
        // TODO also support ECLIPSE_FEATURE? Con: Semantics may be misunderstood to be the same as in target definition files

        public static CapabilityType parsePomValue(String typeString) {
            if (typeString.equals("eclipse-plugin") || typeString.equals("osgi-bundle"))
                return OSGI_BUNDLE;
            else if (typeString.equals("p2-installable-unit"))
                return P2_INSTALLABLE_UNIT;
            else if (typeString.equals("java-package"))
                return JAVA_PACKAGE;
            throw new IllegalArgumentException("Non-recognized capability type: " + typeString);
        }

    }

    public static final class CapabilityPattern {

        private final CapabilityType type;
        private final String id;
        private final String version;
        private final String versionRange;

        public static CapabilityPattern patternWithVersion(CapabilityType type, String id, String version) {
            return new CapabilityPattern(type, id, version, null);
        }

        public static CapabilityPattern patternWithVersionRange(CapabilityType type, String id, String versionRange) {
            return new CapabilityPattern(type, id, null, versionRange);
        }

        public static CapabilityPattern patternWithoutVersion(CapabilityType type, String id) {
            return new CapabilityPattern(type, id, null, null);
        }

        private CapabilityPattern(CapabilityType type, String id, String version, String versionRange) {
            this.type = type;
            this.id = id;
            this.version = version;
            this.versionRange = versionRange;
        }

        public CapabilityType getType() {
            return type;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        public String getVersionRange() {
            return versionRange;
        }

        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(", ", "CapabilityPattern(", ")");
            printMembers(result);
            return result.toString();
        }

        private void printMembers(StringJoiner result) {
            if (type != null)
                result.add("type=" + type);
            if (id != null)
                result.add("id=\"" + id + "\"");
            if (version != null)
                result.add("version=\"" + version + "\"");
            if (versionRange != null)
                result.add("versionRange=\"" + versionRange + "\"");
        }

        public String printMembers() {
            StringJoiner result = new StringJoiner(", ");
            printMembers(result);
            return result.toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, id, version, versionRange);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof CapabilityPattern other && //
                    Objects.equals(this.type, other.type) && //
                    Objects.equals(this.id, other.id) && //
                    Objects.equals(this.version, other.version) && //
                    Objects.equals(this.versionRange, other.versionRange));
        }

    }

}
