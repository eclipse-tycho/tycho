/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Version;

/**
 * Creative copy&paste from org.eclipse.osgi.framework.internal.core.Framework
 * 
 * @author eclipse.org
 * @author igor
 */
public class ExecutionEnvironmentUtils {

    private static String J2SE = "J2SE-"; //$NON-NLS-1$
    private static String JAVASE = "JavaSE-"; //$NON-NLS-1$
    private static String PROFILE_EXT = ".profile"; //$NON-NLS-1$
    private static Map<String, StandardExecutionEnvironment> executionEnvironmentsMap = fillEnvironmentsMap();

    private static Map<String, StandardExecutionEnvironment> fillEnvironmentsMap() {
        Properties listProps = readProperties(findInSystemBundle("profile.list"));
        String[] profileFiles = listProps.getProperty("java.profiles").split(",");
        Map<String, StandardExecutionEnvironment> envMap = new HashMap<String, StandardExecutionEnvironment>();
        for (String profileFile : profileFiles) {
            Properties props = readProperties(findInSystemBundle(profileFile.trim()));
            envMap.put(props.getProperty("osgi.java.profile.name").trim(), new StandardExecutionEnvironment(props));
        }
        return envMap;
    }

    private static Properties readProperties(final URL url) {
        Properties listProps = new Properties();
        InputStream stream = null;
        try {
            stream = url.openStream();
            listProps.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    public static StandardExecutionEnvironment getExecutionEnvironment(String profileName)
            throws UnknownEnvironmentException {
        StandardExecutionEnvironment executionEnvironment = executionEnvironmentsMap.get(profileName);
        if (executionEnvironment == null) {
            throw new UnknownEnvironmentException(profileName);
        }
        return executionEnvironment;
    }

    public static void applyProfileProperties(Properties properties, Properties profileProps) {
        String systemExports = properties.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
        // set the system exports property using the vm profile; only if the property is not already set
        if (systemExports == null) {
            systemExports = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
            if (systemExports != null)
                properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemExports);
        }
        // set the org.osgi.framework.bootdelegation property according to the java profile
        String type = properties.getProperty(Constants.OSGI_JAVA_PROFILE_BOOTDELEGATION); // a null value means ignore
        String profileBootDelegation = profileProps.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
        if (Constants.OSGI_BOOTDELEGATION_OVERRIDE.equals(type)) {
            if (profileBootDelegation == null)
                properties.remove(Constants.FRAMEWORK_BOOTDELEGATION); // override with a null value
            else
                properties.put(Constants.FRAMEWORK_BOOTDELEGATION, profileBootDelegation); // override with the profile value
        } else if (Constants.OSGI_BOOTDELEGATION_NONE.equals(type))
            properties.remove(Constants.FRAMEWORK_BOOTDELEGATION); // remove the bootdelegation property in case it was set
        // set the org.osgi.framework.executionenvironment property according to the java profile
        if (properties.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT) == null) {
            // get the ee from the java profile; if no ee is defined then try the java profile name
            String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
                    profileProps.getProperty(Constants.OSGI_JAVA_PROFILE_NAME));
            if (ee != null)
                properties.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
        }
        if (properties.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES) == null) {
            String systemCapabilities = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
            if (systemCapabilities != null)
                properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities);
        }
    }

    private static URL getNextBestProfile(String javaEdition, Version javaVersion) {
        if (javaVersion == null || (javaEdition != J2SE && javaEdition != JAVASE))
            return null; // we cannot automatically choose the next best profile unless this is a J2SE or JavaSE vm
        URL bestProfile = findNextBestProfile(javaEdition, javaVersion);
        if (bestProfile == null && javaEdition == JAVASE)
            // if this is a JavaSE VM then search for a lower J2SE profile
            bestProfile = findNextBestProfile(J2SE, javaVersion);
        return bestProfile;
    }

    private static URL findNextBestProfile(String javaEdition, Version javaVersion) {
        URL result = null;
        int minor = javaVersion.getMinor();
        do {
            result = findInSystemBundle(javaEdition + javaVersion.getMajor() + "." + minor + PROFILE_EXT); //$NON-NLS-1$
            minor = minor - 1;
        } while (result == null && minor > 0);
        return result;
    }

    private static URL findInSystemBundle(String entry) {
        // Check the ClassLoader in case we're launched off the Java boot classpath
        ClassLoader loader = BundleActivator.class.getClassLoader();
        return loader == null ? ClassLoader.getSystemResource(entry) : loader.getResource(entry);
    }
}
