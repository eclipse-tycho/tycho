/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.core.utils.EEVersion.EEType;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * Represents an OSGi execution environment a.k.a. profile. Execution environments are referenced in
 * MANIFEST.MF using the header "Bundle-RequiredExecutionEnvironment".
 * 
 * See the list of known OSGi profiles in bundle org.eclipse.osgi, file profile.list.
 * 
 */
public class ExecutionEnvironment implements Comparable<ExecutionEnvironment> {

    private String profileName;
    private String compilerSourceLevel;
    private String compilerTargetLevel;
    private String[] systemPackages;
    private EEVersion eeVersion;
    private Properties profileProperties;

    /**
     * Do no instantiate. Use factory method instead
     * {@link ExecutionEnvironmentUtils#getExecutionEnvironment(String)}.
     */
    /* package */ExecutionEnvironment(Properties profileProperties) {
        this.profileName = profileProperties.getProperty("osgi.java.profile.name");
        this.compilerSourceLevel = profileProperties.getProperty("org.eclipse.jdt.core.compiler.source");
        this.compilerTargetLevel = profileProperties
                .getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
        this.systemPackages = profileProperties.getProperty("org.osgi.framework.system.packages").split(",");
        this.eeVersion = parseEEVersion(profileProperties.getProperty("org.osgi.framework.system.capabilities"));
        this.profileProperties = new Properties();
        this.profileProperties.putAll(profileProperties);
    }

    private EEVersion parseEEVersion(String systemCaps) {
        List<EEVersion> eeVersions = new ArrayList<EEVersion>();
        try {
            ManifestElement[] systemCapValues = ManifestElement.parseHeader("org.osgi.framework.system.capabilities",
                    systemCaps);
            for (int i = 0; i < systemCapValues.length; i++) {
                Version version;
                String singleVersion = systemCapValues[i].getAttribute("version:Version");
                if (singleVersion != null) {
                    version = Version.parseVersion(singleVersion);
                } else {
                    String[] versions = systemCapValues[i].getAttribute("version:List<Version>").split(",");
                    List<Version> osgiVersions = new ArrayList<Version>(versions.length);
                    for (String currentVersion : versions) {
                        osgiVersions.add(Version.parseVersion(currentVersion));
                    }
                    version = Collections.max(osgiVersions);
                }
                String execEnv = systemCapValues[i].getAttribute("osgi.ee");
                eeVersions.add(new EEVersion(version, EEType.fromName(execEnv)));
            }
            return Collections.max(eeVersions);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    public String getProfileName() {
        return profileName;
    }

    public String getCompilerSourceLevel() {
        return compilerSourceLevel;
    }

    public String getCompilerTargetLevel() {
        return compilerTargetLevel;
    }

    /*
     * for debug purposes
     */
    @Override
    public String toString() {
        return "OSGi profile '" + getProfileName() + "' { source level: " + compilerSourceLevel + ", target level: "
                + compilerTargetLevel + "}";
    }

    public String[] getSystemPackages() {
        return systemPackages;
    }

    public int compareTo(ExecutionEnvironment otherEnv) {
        return eeVersion.compareTo(otherEnv.eeVersion);
    }

    public Properties getProfileProperties() {
        return profileProperties;
    }

    /**
     * Returns <code>true</code> if classes compiled for the specified target can be executed in
     * this execution environment or if this environment's compiler target compatibility is unknown.
     */
    public boolean isCompatibleCompilerTargetLevel(String target) {
        if (target == null) {
            throw new IllegalArgumentException();
        }
        if (compilerTargetLevel == null) {
            return true;
        }

        try {
            Version thisTargetVersion = Version.parseVersion(compilerTargetLevel);

            if ("jsr14".equalsIgnoreCase(target)) {
                target = "1.4";
            }

            Version targetVersion = Version.parseVersion(target);

            return thisTargetVersion.compareTo(targetVersion) >= 0;
        } catch (IllegalArgumentException e) {
            // we could not parse one or both of the provided target level, assume they are incompatible 
            return false;
        }
    }
}
