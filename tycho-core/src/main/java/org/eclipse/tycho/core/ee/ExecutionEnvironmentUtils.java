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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.util.ManifestElement;
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
    private static Map<String, ExecutionEnvironment> executionEnvironmentsMap = fillEnvironmentsMap();

    private static Map<String, ExecutionEnvironment> fillEnvironmentsMap() {
        Properties listProps = readProperties(findInSystemBundle("profile.list"));
        String[] profileFiles = listProps.getProperty("java.profiles").split(",");
        Map<String, ExecutionEnvironment> envMap = new HashMap<String, ExecutionEnvironment>();
        for (String profileFile : profileFiles) {
            Properties props = readProperties(findInSystemBundle(profileFile.trim()));
            envMap.put(props.getProperty("osgi.java.profile.name").trim(), new ExecutionEnvironment(props));
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
    public static ExecutionEnvironment getExecutionEnvironment(String profileName) throws UnknownEnvironmentException {
        ExecutionEnvironment executionEnvironment = executionEnvironmentsMap.get(profileName);
        if (executionEnvironment == null) {
            throw new UnknownEnvironmentException(profileName);
        }
        return executionEnvironment;
    }

    public static void loadVMProfile(Properties properties) {
        applyProfileProperties(properties, findVMProfile(properties));
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
    }

    private static Properties findVMProfile(Properties properties) {
        Properties result = new Properties();
        // Find the VM profile name using J2ME properties
        String j2meConfig = properties.getProperty(Constants.J2ME_MICROEDITION_CONFIGURATION);
        String j2meProfiles = properties.getProperty(Constants.J2ME_MICROEDITION_PROFILES);
        String vmProfile = null;
        String javaEdition = null;
        Version javaVersion = null;
        if (j2meConfig != null && j2meConfig.length() > 0 && j2meProfiles != null && j2meProfiles.length() > 0) {
            // save the vmProfile based off of the config and profile
            // use the last profile; assuming that is the highest one
            String[] j2meProfileList = ManifestElement.getArrayFromList(j2meProfiles, " "); //$NON-NLS-1$
            if (j2meProfileList != null && j2meProfileList.length > 0)
                vmProfile = j2meConfig + '_' + j2meProfileList[j2meProfileList.length - 1];
        } else {
            // No J2ME properties; use J2SE properties
            // Note that the CDC spec appears not to require VM implementations to set the
            // javax.microedition properties!!  So we will try to fall back to the 
            // java.specification.name property, but this is pretty ridiculous!!
            String javaSpecVersion = properties.getProperty("java.specification.version"); //$NON-NLS-1$
            // set the profile and EE based off of the java.specification.version
            // TODO We assume J2ME Foundation and J2SE here.  need to support other profiles J2EE ...
            if (javaSpecVersion != null) {
                StringTokenizer st = new StringTokenizer(javaSpecVersion, " _-"); //$NON-NLS-1$
                javaSpecVersion = st.nextToken();
                String javaSpecName = properties.getProperty("java.specification.name"); //$NON-NLS-1$
                if ("J2ME Foundation Specification".equals(javaSpecName)) //$NON-NLS-1$
                    vmProfile = "CDC-" + javaSpecVersion + "_Foundation-" + javaSpecVersion; //$NON-NLS-1$ //$NON-NLS-2$
                else {
                    // look for JavaSE if 1.6 or greater; otherwise look for J2SE
                    Version v16 = new Version("1.6"); //$NON-NLS-1$
                    javaEdition = J2SE;
                    try {
                        javaVersion = new Version(javaSpecVersion);
                        if (v16.compareTo(javaVersion) <= 0)
                            javaEdition = JAVASE;
                    } catch (IllegalArgumentException e) {
                        // do nothing
                    }
                    vmProfile = javaEdition + javaSpecVersion;
                }
            }
        }
        URL url = null;
        // check for the java profile property for a url
        String propJavaProfile = FrameworkProperties.getProperty(Constants.OSGI_JAVA_PROFILE);
        if (propJavaProfile != null)
            try {
                // we assume a URL
                url = new URL(propJavaProfile);
            } catch (MalformedURLException e1) {
                // try using a relative path in the system bundle
                url = findInSystemBundle(propJavaProfile);
            }
        if (url == null && vmProfile != null) {
            // look for a profile in the system bundle based on the vm profile
            String javaProfile = vmProfile + PROFILE_EXT;
            url = findInSystemBundle(javaProfile);
            if (url == null)
                url = getNextBestProfile(javaEdition, javaVersion);
        }
        if (url == null)
            // the profile url is still null then use the osgi min profile in OSGi by default
            url = findInSystemBundle("OSGi_Minimum-1.1.profile"); //$NON-NLS-1$
        if (url != null) {
            InputStream in = null;
            try {
                in = url.openStream();
                result.load(new BufferedInputStream(in));
            } catch (IOException e) {
                // TODO consider logging ...
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException ee) {
                        // do nothing
                    }
            }
        }
        // set the profile name if it does not provide one
        if (result.getProperty(Constants.OSGI_JAVA_PROFILE_NAME) == null)
            if (vmProfile != null)
                result.put(Constants.OSGI_JAVA_PROFILE_NAME, vmProfile.replace('_', '/'));
            else
                // last resort; default to the absolute minimum profile name for the framework
                result.put(Constants.OSGI_JAVA_PROFILE_NAME, "OSGi/Minimum-1.1"); //$NON-NLS-1$
        return result;
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
