/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.tycho.test.AbstractTychoIntegrationTest;

/**
 * Provides system properties and certain properties from the test code build ("outer build"), like
 * the location of the local Maven repository. For this class to work, the outer build must
 * configure the <tt>maven-properties-plugin</tt> to capture the values in a file called
 * baseTest.properties (see tycho-its/pom.xml for an example).
 */
public class EnvironmentUtil {

    private static final Properties props;

    static {
        props = new Properties();
        ClassLoader cl = AbstractTychoIntegrationTest.class.getClassLoader();
        InputStream is = cl.getResourceAsStream("baseTest.properties");
        if (is != null) {
            try {
                try {
                    props.load(is);
                } finally {
                    is.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static synchronized String getProperty(String key) {
        return props.getProperty(key);
    }

    private static final String WINDOWS_OS = "windows";

    private static final String MAC_OS = "mac os x";

    private static final String MAC_OS_DARWIN = "darwin";

    private static final String LINUX_OS = "linux";

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.startsWith(WINDOWS_OS);
    }

    public static boolean isLinux() {
        return OS.startsWith(LINUX_OS);
    }

    public static boolean isMac() {
        return OS.startsWith(MAC_OS) || OS.startsWith(MAC_OS_DARWIN);
    }

    // TODO find a more reliable way
    public static boolean isEclipse32Platform() {
        return new File(getTargetPlatform(), "startup.jar").exists();
    }

    public static String getTargetPlatform() {
        return getProperty("its-target-platform");
    }

    public static String getTestSettings() {
        String value = getProperty("its-settings");
        if (value == null || value.contains("$"))
            return null;
        return value;
    }

    public static String getMavenHome() {
        return getProperty("maven-dir");
    }

    public static String getTychoVersion() {
        return getProperty("tycho-version");
    }

    public static int getHttpServerPort() {
        String port = getProperty("server-port");
        return Integer.parseInt(port);
    }

    public static String getLocalRepo() {
        return getProperty("local-repo");
    }

}
