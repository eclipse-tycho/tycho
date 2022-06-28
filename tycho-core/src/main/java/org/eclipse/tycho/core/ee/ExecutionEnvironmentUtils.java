/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment.JavaInfo;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment.SystemPackageEntry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;

/**
 * Creative copy&paste from org.eclipse.osgi.framework.internal.core.Framework
 * 
 * @author eclipse.org
 * @author igor
 */
public class ExecutionEnvironmentUtils {

    private static Map<String, StandardExecutionEnvironment> executionEnvironmentsMap;

    private static final Map<String, StandardExecutionEnvironment> surrogateExecutionEnvironmentsMap = new ConcurrentHashMap<>();

    private static Properties readProperties(final URL url) {
        Properties listProps = new Properties();
        try (InputStream stream = url.openStream()) {
            listProps.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return listProps;
    }

    /**
     * Get the execution environment for the specified OSGi profile name.
     * 
     * @param profileName
     *            profile name value as specified for key "Bundle-RequiredExecutionEnvironment" in
     *            MANIFEST.MF
     * @return the corresponding {@link ExecutionEnvironment}.
     * @throws UnknownEnvironmentException
     *             if profileName is unknown.
     */
    public static @Nonnull StandardExecutionEnvironment getExecutionEnvironment(String profileName,
            ToolchainManager manager, MavenSession session, Logger logger) throws UnknownEnvironmentException {
        Map<String, StandardExecutionEnvironment> map = getExecutionEnvironmentsMap(manager, session, logger);
        StandardExecutionEnvironment ee = map.get(profileName);
        if (ee != null) {
            return ee;
        }
        int version = getVersion(profileName);
        if (version > 8) {
            //try find newer version...
            StandardExecutionEnvironment higherEE = map.keySet().stream()
                    .mapToInt(ExecutionEnvironmentUtils::getVersion).filter(v -> v > version).min().stream()
                    .mapToObj(v -> {
                        String[] split = profileName.split("-");
                        return split[0] + "-" + v;
                    }).map(map::get).findFirst().orElse(null);
            if (higherEE != null) {
                logger.warn("Using " + higherEE.getProfileName() + " to fulfill requested profile of " + profileName
                        + " this might lead to faulty dependency resolution, consider define a suitable JDK in the toolchains.xml");
                return getSurrogate(profileName, higherEE);
            }
        }
        logger.debug("Unknown OSGi execution environment, EE currently known to the build:");
        for (StandardExecutionEnvironment knownEE : map.values()) {
            logger.debug(knownEE.getProfileName());
        }
        throw new UnknownEnvironmentException(profileName);
    }

    private static StandardExecutionEnvironment getSurrogate(String profileName,
            StandardExecutionEnvironment surrogateEE) {
        return surrogateExecutionEnvironmentsMap.computeIfAbsent(surrogateEE.getProfileName() + " as " + profileName,
                nil -> {
                    List<String> packages = Arrays
                            .stream(surrogateEE.getProfileProperties()
                                    .getProperty("org.osgi.framework.system.packages", "").split(","))
                            .map(String::trim).collect(Collectors.toList());
                    Properties profileProperties = createProfileJvm(getVersion(profileName), packages);
                    return new StandardExecutionEnvironment(profileProperties, surrogateEE.getToolchain(),
                            surrogateEE.getLogger());
                });
    }

    public static Collection<String> getProfileNames(ToolchainManager manager, MavenSession session, Logger logger) {

        return new ArrayList<>(getExecutionEnvironmentsMap(manager, session, logger).keySet());
    }

    private static synchronized Map<String, StandardExecutionEnvironment> getExecutionEnvironmentsMap(
            ToolchainManager manager, MavenSession session, Logger logger) {
        if (executionEnvironmentsMap == null) {
            executionEnvironmentsMap = new LinkedHashMap<>();
            Properties listProps = readProperties(findInSystemBundle("profile.list"));
            //first read all profiles that are part of the system...
            for (String profileFile : listProps.getProperty("java.profiles").split(",")) {
                Properties props = readProperties(findInSystemBundle(profileFile.trim()));
                if (props == null) {
                    logger.warn("can't read profile " + profileFile + " from system path");
                    continue;
                }
                String name = props.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME).trim();
                executionEnvironmentsMap.put(name, new StandardExecutionEnvironment(props,
                        getToolchainFor(name, manager, session, logger), logger));
            }
            //derive from the toolchains...
            if (manager != null) {
                List<Toolchain> jdks = manager.getToolchains(session, "jdk", null);
                for (Toolchain jdk : jdks) {
                    JavaInfo javaInfo = StandardExecutionEnvironment.readFromToolchains(jdk, logger);
                    if (javaInfo.version > 8) {
                        Properties toolchainJvm = createProfileJvm(javaInfo.version, javaInfo.packages);
                        String name = toolchainJvm.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME).trim();
                        executionEnvironmentsMap.put(name, new StandardExecutionEnvironment(toolchainJvm, jdk, logger));
                    }
                }
            }
            //derive from the running jvm...
            int javaVersion = Runtime.version().feature();
            if (!executionEnvironmentsMap.containsKey("JavaSE-" + javaVersion)) {
                Properties runningVm = createProfileJvm(javaVersion, ListSystemPackages.getCurrentJREPackages());
                String name = runningVm.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME).trim();
                executionEnvironmentsMap.put(name, new StandardExecutionEnvironment(runningVm,
                        getToolchainFor(name, manager, session, logger), logger));
            }
        }
        return executionEnvironmentsMap;
    }

    public static Toolchain getToolchainFor(String profileName, ToolchainManager manager, MavenSession session,
            Logger logger) {
        if (manager != null) {
            logger.debug("Search profile " + profileName + " in ToolchainManager...");
            //First try to find it by ID
            for (Toolchain toolchain : manager.getToolchains(session, "jdk",
                    Collections.singletonMap("id", profileName))) {
                return toolchain;
            }
            //Try find by version
            int version = getVersion(profileName);
            if (version > 8) {
                for (Toolchain toolchain : manager.getToolchains(session, "jdk",
                        Collections.singletonMap("version", String.valueOf(version)))) {
                    return toolchain;
                }
            }
        }
        return null;
    }

    public static int getVersion(String profileName) {
        String[] split = profileName.split("-");
        if (split.length == 2) {
            try {
                return (int) Double.parseDouble(split[split.length - 1]);
            } catch (NumberFormatException e) {
                //can't check then...
            }
        }
        return -1;

    }

    public static void applyProfileProperties(Properties properties, ExecutionEnvironment executionEnvironment) {
        String systemExports = properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
        // set the system exports property using the vm profile; only if the property is not already set
        if (systemExports == null) {
            systemExports = executionEnvironment.getSystemPackages().stream()
                    .map(SystemPackageEntry::toPackageSpecifier).collect(Collectors.joining(","));
            if (systemExports != null && !systemExports.isEmpty())
                properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemExports);
        }
        // set the org.osgi.framework.bootdelegation property according to the java profile
        String type = properties.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_BOOTDELEGATION); // a null value means ignore
        Properties profileProps = executionEnvironment.getProfileProperties();
        String profileBootDelegation = profileProps.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
        if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_OVERRIDE.equals(type)) {
            if (profileBootDelegation == null)
                properties.remove(Constants.FRAMEWORK_BOOTDELEGATION); // override with a null value
            else
                properties.put(Constants.FRAMEWORK_BOOTDELEGATION, profileBootDelegation); // override with the profile value
        } else if (EquinoxConfiguration.PROP_OSGI_BOOTDELEGATION_NONE.equals(type))
            properties.remove(Constants.FRAMEWORK_BOOTDELEGATION); // remove the bootdelegation property in case it was set
        // set the org.osgi.framework.executionenvironment property according to the java profile
        if (properties.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT) == null) {
            // get the ee from the java profile; if no ee is defined then try the java profile name
            String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
                    profileProps.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME));
            if (ee != null)
                properties.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
        }
        if (properties.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES) == null) {
            String systemCapabilities = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
            if (systemCapabilities != null)
                properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities);
        }
    }

    private static URL findInSystemBundle(String entry) {
        // Check the ClassLoader in case we're launched off the Java boot classpath
        ClassLoader loader = BundleActivator.class.getClassLoader();
        return loader == null ? ClassLoader.getSystemResource(entry) : loader.getResource(entry);
    }

    //This is derived from org.eclipse.equinox.p2.publisher.actions.JREAction.createDefaultProfileFromRunningJvm()
    private static Properties createProfileJvm(int javaVersion, Collection<String> packages) {
        String profileName = "JavaSE-" + javaVersion;
        Properties props = new Properties();
        // add systempackages
        props.setProperty("org.osgi.framework.system.packages", packages.stream().collect(Collectors.joining(",")));
        // add EE
        StringBuilder ee = new StringBuilder(
                "OSGi/Minimum-1.0,OSGi/Minimum-1.1,OSGi/Minimum-1.2,JavaSE/compact1-1.8,JavaSE/compact2-1.8,JavaSE/compact3-1.8,JRE-1.1,J2SE-1.2,J2SE-1.3,J2SE-1.4,J2SE-1.5,JavaSE-1.6,JavaSE-1.7,JavaSE-1.8,");
        for (int i = 9; i < javaVersion; i++) {
            ee.append("JavaSE-" + String.valueOf(i) + ",");
        }
        ee.append(profileName);
        props.setProperty("org.osgi.framework.executionenvironment", ee.toString());
        // add capabilities
        StringBuilder versionList = new StringBuilder();
        versionList.append("1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8");
        for (int i = 9; i <= javaVersion; i++) {
            versionList.append(", " + String.valueOf(i) + ".0");
        }
        props.setProperty("org.osgi.framework.system.capabilities",
                "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0, 1.1, 1.2\",osgi.ee; osgi.ee=\"JRE\"; version:List<Version>=\"1.0, 1.1\",osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\""
                        + versionList.toString()
                        + "\",osgi.ee; osgi.ee=\"JavaSE/compact1\"; version:List<Version>=\"1.8,"
                        + String.valueOf(javaVersion)
                        + ".0\",osgi.ee; osgi.ee=\"JavaSE/compact2\"; version:List<Version>=\"1.8,"
                        + String.valueOf(javaVersion)
                        + ".0\",osgi.ee; osgi.ee=\"JavaSE/compact3\"; version:List<Version>=\"1.8,"
                        + String.valueOf(javaVersion) + ".0\"");

        // add profile name and compiler options
        props.setProperty("osgi.java.profile.name", profileName);
        props.setProperty("org.eclipse.jdt.core.compiler.compliance", String.valueOf(javaVersion));
        props.setProperty("org.eclipse.jdt.core.compiler.source", String.valueOf(javaVersion));
        props.setProperty("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode", "enabled");
        props.setProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform", String.valueOf(javaVersion));
        props.setProperty("org.eclipse.jdt.core.compiler.problem.assertIdentifier", "error");
        props.setProperty("org.eclipse.jdt.core.compiler.problem.enumIdentifier", "error");
        return props;
    }
}
