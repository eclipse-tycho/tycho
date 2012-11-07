/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.core.ee.EEVersion.EEType;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * Represents a standard OSGi execution environment profile. See the list of known OSGi profiles in
 * bundle org.eclipse.osgi, file profile.list.
 * 
 * Standard execution environment capabilities can be referenced in MANIFEST.MF using the header
 * "Bundle-RequiredExecutionEnvironment". In order to pick the minimal, required profile from the
 * alternatives listed in a BREE header, instances of this class have a total ordering.
 * 
 * TODO This class both represents an EE profile (i.e. the concrete EE implementation used by the
 * compiler) and an EE capability (i.e. the capability required via the BREE). This should be
 * separate classes. (An EE profile typically provides multiple EE capabilities.)
 */
public class StandardExecutionEnvironment implements Comparable<StandardExecutionEnvironment>, ExecutionEnvironment {

    private static final Map<String, String> TARGET_ALIASES;

    static {
        // http://help.eclipse.org/juno/topic/org.eclipse.jdt.doc.user/tasks/task-using_batch_compiler.htm

        Map<String, String> targetAliases = new HashMap<String, String>();
        targetAliases.put("jsr14", "1.4");
        targetAliases.put("5", "1.5");
        targetAliases.put("5.0", "1.5");
        targetAliases.put("6", "1.6");
        targetAliases.put("6.0", "1.6");
        targetAliases.put("7", "1.7");
        targetAliases.put("7.0", "1.7");
        TARGET_ALIASES = Collections.unmodifiableMap(targetAliases);
    }

    private String profileName;
    private String compilerSourceLevel;
    private String compilerTargetLevel;
    private Set<String> systemPackages;
    private EEVersion eeVersion;
    private Properties profileProperties;

    /**
     * Do no instantiate. Use factory method instead
     * {@link ExecutionEnvironmentUtils#getExecutionEnvironment(String)}.
     */
    /* package */StandardExecutionEnvironment(Properties profileProperties) {
        this.profileName = profileProperties.getProperty("osgi.java.profile.name");
        this.compilerSourceLevel = profileProperties.getProperty("org.eclipse.jdt.core.compiler.source");
        this.compilerTargetLevel = profileProperties
                .getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
        this.systemPackages = new LinkedHashSet<String>(Arrays.asList(profileProperties.getProperty(
                "org.osgi.framework.system.packages").split(",")));
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

    public String getCompilerSourceLevelDefault() {
        return compilerSourceLevel;
    }

    public String getCompilerTargetLevelDefault() {
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

    public Set<String> getSystemPackages() {
        return systemPackages;
    }

    public int compareTo(StandardExecutionEnvironment otherEnv) {
        return eeVersion.compareTo(otherEnv.eeVersion);
    }

    public Properties getProfileProperties() {
        return profileProperties;
    }

    public boolean isCompatibleCompilerTargetLevel(String target) {
        if (target == null) {
            throw new IllegalArgumentException();
        }
        if (compilerTargetLevel == null) {
            return true;
        }

        try {
            Version thisTargetVersion = toTargetVersion(compilerTargetLevel);
            Version targetVersion = toTargetVersion(target);
            return thisTargetVersion.compareTo(targetVersion) >= 0;
        } catch (IllegalArgumentException e) {
            // we could not parse one or both of the provided target level, assume they are incompatible 
            return false;
        }
    }

    private static Version toTargetVersion(String target) {
        String targetAlias = TARGET_ALIASES.get(target.trim().toLowerCase());
        if (targetAlias != null) {
            target = targetAlias;
        }
        return Version.parseVersion(target);
    }
}
