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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
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

    private static final Map<String, Properties> profilesProperties = fillEnvironmentsMap();
    private static final Map<String, StandardExecutionEnvironment> executionEnvironmentsMap = new ConcurrentHashMap<>(
            profilesProperties.size(), 1.f);

    private static Map<String, Properties> fillEnvironmentsMap() {
        Properties listProps = readProperties(findInSystemBundle("profile.list"));
        List<String> profileFiles = new ArrayList<>(Arrays.asList(listProps.getProperty("java.profiles").split(",")));
        profileFiles.add("JavaSE-11.profile");
        profileFiles.add("JavaSE-17.profile");
        profileFiles.add("JavaSE-18.profile");
        Map<String, Properties> envMap = new LinkedHashMap<>(profileFiles.size(), 1.f);
        for (String profileFile : profileFiles) {
            Properties props = readProperties(findInSystemBundle(profileFile.trim()));
            envMap.put(props.getProperty(EquinoxConfiguration.PROP_OSGI_JAVA_PROFILE_NAME).trim(), props);
        }
        return envMap;
    }

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
    public static StandardExecutionEnvironment getExecutionEnvironment(String profileName, ToolchainManager manager,
            MavenSession session, Logger logger) throws UnknownEnvironmentException {
        if (!profilesProperties.containsKey(profileName)) {
            throw new UnknownEnvironmentException(profileName);
        }
        return executionEnvironmentsMap.computeIfAbsent(profileName, name -> {
            List<Toolchain> toolchains = manager != null && session != null
                    ? manager.getToolchains(session, "jdk", Collections.singletonMap("id", profileName))
                    : Collections.emptyList();
            return new StandardExecutionEnvironment(profilesProperties.get(name),
                    toolchains.isEmpty() ? null : toolchains.iterator().next(), logger);
        });
    }

    public static List<String> getProfileNames() {
        return new ArrayList<>(profilesProperties.keySet());
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
}
