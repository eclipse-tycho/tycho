/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *     Bachmann electronic GmbH. - Support for ignoreError flag
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.mirroring.facade;

import java.util.HashMap;
import java.util.Map;

/**
 * Various options which can be configured for mirroring. See
 * org.eclipse.equinox.p2.internal.repository.tools.SlicingOptions.
 */
public class MirrorOptions {

    private static final String INSTALL_FEATURES = "org.eclipse.update.install.features";

    private boolean followStrictOnly = false;
    private boolean includeOptional = true;
    private boolean includeNonGreedy = true;
    private boolean includePacked = true;
    private boolean followOnlyFilteredRequirements = false;
    private boolean latestVersionOnly = false;
    private Map<String, String> filter = new HashMap<>();
    private boolean ignoreErrors = false;

    /**
     * Creates mirror options with default values.
     */
    public MirrorOptions() {
        setIncludeFeatures(true);
    }

    public boolean isFollowStrictOnly() {
        return followStrictOnly;
    }

    /**
     * Set to true if only strict dependencies should be followed. A strict dependency is defined by a
     * version range only including one version (e.g. [1.0.0.v2009, 1.0.0.v2009]). (Default is false)
     */
    public void setFollowStrictOnly(boolean followStrictOnly) {
        this.followStrictOnly = followStrictOnly;
    }

    public boolean isIncludeOptional() {
        return includeOptional;
    }

    /**
     * Whether or not to follow optional requirements. (Default is true).
     */
    public void setIncludeOptional(boolean includeOptional) {
        this.includeOptional = includeOptional;
    }

    public boolean isIncludeNonGreedy() {
        return includeNonGreedy;
    }

    public boolean isIncludePacked() {
        return includePacked;
    }

    /**
     * Whether or not to follow non-greedy requirements. (Default is true).
     */
    public void setIncludeNonGreedy(boolean includeNonGreedy) {
        this.includeNonGreedy = includeNonGreedy;
    }

    public boolean isIncludeFeatures() {
        return Boolean.valueOf(filter.get(INSTALL_FEATURES));
    }

    public void setIncludeFeatures(boolean includeFeatures) {
        filter.put(INSTALL_FEATURES, String.valueOf(includeFeatures));
    }

    public boolean isFollowOnlyFilteredRequirements() {
        return followOnlyFilteredRequirements;
    }

    /**
     * Whether to follow only requirements which match the given filter.
     */
    public void setFollowOnlyFilteredRequirements(boolean followOnlyFilteredRequirements) {
        this.followOnlyFilteredRequirements = followOnlyFilteredRequirements;
    }

    public boolean isLatestVersionOnly() {
        return latestVersionOnly;
    }

    /**
     * Set to "true" to filter the resulting set of IUs to only included the latest version of each
     * Installable Unit. By default, all versions satisfying dependencies are included.
     */
    public void setLatestVersionOnly(boolean latestVersionOnly) {
        this.latestVersionOnly = latestVersionOnly;
    }

    /**
     * Filter properties
     */
    public Map<String, String> getFilter() {
        return filter;
    }

    public void setPlatformFilter(String os, String ws, String arch) {
        filter.put("osgi.os", os);
        filter.put("osgi.ws", ws);
        filter.put("osgi.arch", arch);
    }

    public void setIncludePacked(boolean includePacked) {
        this.includePacked = includePacked;
    }

    /**
     * When set to true,the mirroring application continues to run in the event of an error during the
     * mirroring process. (Default: false)
     */
    public void setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }

    public boolean isIgnoreErrors() {
        return this.ignoreErrors;
    }
}
