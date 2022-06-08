/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     Christoph Läubrich - #496 - ResolutionArguments#hashcode is not stable
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.core.ee.EEVersion.EEType;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
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
        // https://help.eclipse.org/juno/topic/org.eclipse.jdt.doc.user/tasks/task-using_batch_compiler.htm

        Map<String, String> targetAliases = new HashMap<>();
        targetAliases.put("jsr14", "1.4");
        targetAliases.put("5", "1.5");
        targetAliases.put("5.0", "1.5");
        targetAliases.put("6", "1.6");
        targetAliases.put("6.0", "1.6");
        targetAliases.put("7", "1.7");
        targetAliases.put("7.0", "1.7");
        targetAliases.put("8", "1.8");
        targetAliases.put("8.0", "1.8");
        targetAliases.put("9", "1.9");
        targetAliases.put("9.0", "1.9");
        targetAliases.put("10", "1.10");
        targetAliases.put("10.0", "1.10");
        TARGET_ALIASES = Collections.unmodifiableMap(targetAliases);
    }

    private final String profileName;
    private final String compilerSourceLevel;
    private final String compilerTargetLevel;
    private List<SystemPackageEntry> systemPackages;
    private final EEVersion eeVersion;
    private final Properties profileProperties;
    private final Toolchain toolchain;
    private Logger logger;

    /**
     * Do no instantiate. Use factory method instead
     * {@link ExecutionEnvironmentUtils#getExecutionEnvironment(String)}.
     */
    @Deprecated
    StandardExecutionEnvironment(@Nonnull Properties profileProperties) {
        this(profileProperties, null, null);
    }

    /* package */ StandardExecutionEnvironment(@Nonnull Properties profileProperties, @Nullable Toolchain toolchain,
            @Nullable Logger logger) {
        this.toolchain = toolchain;
        this.profileName = profileProperties.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME);
        this.compilerSourceLevel = profileProperties.getProperty("org.eclipse.jdt.core.compiler.source");
        this.compilerTargetLevel = profileProperties
                .getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform");
        this.eeVersion = parseEEVersion(profileProperties.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
        this.profileProperties = new Properties();
        this.profileProperties.putAll(profileProperties);
        this.logger = logger;
    }

    static JavaInfo readFromToolchains(Toolchain toolchain, Logger logger) {
        if (toolchain == null) {
            return new JavaInfo(-1, Collections.emptySet());
        }
        String java = toolchain.findTool("java");
        if (java == null) {
            return new JavaInfo(-1, Collections.emptySet());
        }
        Set<String> res = new HashSet<>();
        int version = -1;
        try {
            ProcessBuilder builder = new ProcessBuilder(java, "-jar",
                    getSystemPackagesCompanionJar().getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(builder.start().getInputStream(), Charset.defaultCharset()))) {
                String line = reader.readLine();
                try {
                    if (line != null) {
                        //for old vms < java 9 we might get no response at all
                        version = Integer.parseInt(line);
                    }
                } catch (NumberFormatException e) {
                    StringBuilder sb = new StringBuilder(line);
                    while ((line = reader.readLine()) != null) {
                        sb.append(System.lineSeparator());
                        sb.append(line);
                    }
                    logger.debug("[ReadPackagesFromToolchains] Can't read java version for " + java
                            + ", full output was: " + sb);
                    return new JavaInfo(-1, List.of());
                }
                while ((line = reader.readLine()) != null) {
                    res.add(line);
                }
            }
        } catch (IOException e) {
            logger.error("[ReadPackagesFromToolchains] start JVM process for " + java + " failed: " + e);
        }
        return new JavaInfo(version, res);
    }

    static final class JavaInfo {
        final int version;
        final Collection<String> packages;

        private JavaInfo(int version, Collection<String> packages) {
            this.version = version;
            this.packages = Collections.unmodifiableCollection(packages);
        }

    }

    static File getSystemPackagesCompanionJar() throws IOException {
        File companionFile = File.createTempFile("tycho-system-packages-companion", ".jar");
        companionFile.deleteOnExit();
        try (InputStream contents = ListSystemPackages.class.getClassLoader()
                .getResourceAsStream("system-packages-companion.jar")) {
            FileUtils.copyToFile(contents, companionFile);
        }
        return companionFile;
    }

    private static EEVersion parseEEVersion(String systemCaps) {
        List<EEVersion> eeVersions = new ArrayList<>();
        try {
            ManifestElement[] systemCapValues = ManifestElement.parseHeader("org.osgi.framework.system.capabilities",
                    systemCaps);
            for (ManifestElement systemCapValue : systemCapValues) {
                Version version;
                String singleVersion = systemCapValue.getAttribute("version:Version");
                if (singleVersion != null) {
                    version = Version.parseVersion(singleVersion);
                } else {
                    String[] versions = systemCapValue.getAttribute("version:List<Version>").split(",");
                    List<Version> osgiVersions = new ArrayList<>(versions.length);
                    for (String currentVersion : versions) {
                        osgiVersions.add(Version.parseVersion(currentVersion));
                    }
                    version = Collections.max(osgiVersions);
                }
                String execEnv = systemCapValue.getAttribute("osgi.ee");
                EEType eeType = EEType.fromName(execEnv);
                if (eeType != null) {
                    eeVersions.add(new EEVersion(version, eeType));
                }
            }
            return Collections.max(eeVersions);
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public String getCompilerSourceLevelDefault() {
        return compilerSourceLevel;
    }

    @Override
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

    @Override
    public synchronized Collection<SystemPackageEntry> getSystemPackages() {
        if (systemPackages == null) {
            // EE definitions in Tycho for JVMs 11+ will no longer contain system packages as with modular JVMs it's not sure
            // all packages will be available at runtime
            if (profileProperties.containsKey(Constants.FRAMEWORK_SYSTEMPACKAGES)) {
                logger.debug("Found system.packages in profile definition file for " + profileName + ",");
                String systemPackagesValue = profileProperties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
                if (systemPackagesValue.isBlank()) {
                    this.systemPackages = Collections.emptyList();
                } else {
                    try {
                        this.systemPackages = Arrays.stream(
                                ManifestElement.parseHeader(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackagesValue))
                                .map(jrePackage -> {
                                    String packageName = jrePackage.getValue();
                                    String version = jrePackage.getAttribute("version");
                                    return new SystemPackageEntry(packageName, version);
                                }).collect(Collectors.toList());
                    } catch (BundleException e) {
                        logger.error(e.getMessage(), e);
                        this.systemPackages = Collections.emptyList();
                    }
                }
            } else if (toolchain != null) {
                logger.debug(
                        "No system.packages in profile definition file for " + profileName + "; checking toolchain.");
                this.systemPackages = readFromToolchains(toolchain, logger).packages.stream()
                        .map(packageName -> new SystemPackageEntry(packageName, null)).collect(Collectors.toList());
            } else if (Integer.parseInt(compilerSourceLevel) == Runtime.version().feature()) {
                logger.debug("Currently running JRE matches source level for " + getProfileName()
                        + "; current JRE system packages are used.");
                this.systemPackages = ListSystemPackages.getCurrentJREPackages().stream()
                        .map(packageName -> new SystemPackageEntry(packageName, null)).collect(Collectors.toList());
            }
            if (this.systemPackages == null || this.systemPackages.isEmpty()) {
                logger.warn("No system packages found in profile nor toolchain for " + profileName
                        + ", using current JRE system packages.\n"
                        + "This can cause faulty dependency resolution, consider adding a definition for a 'jdk' with id="
                        + profileName + " in your toolchains.xml");
                this.systemPackages = ListSystemPackages.getCurrentJREPackages().stream()
                        .map(packageName -> new SystemPackageEntry(packageName, null)).collect(Collectors.toList());
            }
        }
        return systemPackages;
    }

    @Override
    public int compareTo(StandardExecutionEnvironment otherEnv) {
        return eeVersion.compareTo(otherEnv.eeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(compilerSourceLevel, compilerTargetLevel, eeVersion, profileName, profileProperties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StandardExecutionEnvironment)) {
            return false;
        }
        StandardExecutionEnvironment other = (StandardExecutionEnvironment) obj;
        return Objects.equals(this.compilerSourceLevel, other.compilerSourceLevel)
                && Objects.equals(this.compilerTargetLevel, other.compilerTargetLevel)
                && Objects.equals(this.eeVersion, other.eeVersion)
                && Objects.equals(this.profileName, other.profileName)
                && Objects.equals(this.profileProperties, other.profileProperties);
    }

    @Override
    public Properties getProfileProperties() {
        return profileProperties;
    }

    @Override
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

    Toolchain getToolchain() {
        return toolchain;
    }

    Logger getLogger() {
        return logger;
    }
}
