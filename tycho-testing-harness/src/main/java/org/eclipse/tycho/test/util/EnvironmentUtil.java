/*******************************************************************************
 * Copyright (c) 2008, 2023 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;

/**
 * Provides system properties and certain properties from the test code build ("outer build"), like
 * the location of the local Maven repository. For this class to work, the outer build must
 * configure the <code>maven-properties-plugin</code> to capture the values in a file called
 * baseTest.properties (see tycho-its/pom.xml for an example).
 */
public class EnvironmentUtil {

    private static final String MAVEN_HOME_INFO = "Maven home:";

    private static final Properties props;

    static {
        props = new Properties();
        ClassLoader cl = AbstractTychoIntegrationTest.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream("baseTest.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static synchronized String getProperty(String key) {
        return props.getProperty(key);
    }

    private static final String WINDOWS_OS = "windows";

    private static final String MAC_OS = "mac os x";

    private static final String MAC_OS_DARWIN = "darwin";

    private static final String LINUX_OS = "linux";

    private static final String FREEBSD_OS = "freebsd";

    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.startsWith(WINDOWS_OS);
    }

    public static boolean isLinux() {
        return OS.startsWith(LINUX_OS);
    }

    public static boolean isFreeBSD() {
        return OS.startsWith(FREEBSD_OS);
    }

    public static boolean isMac() {
        return OS.startsWith(MAC_OS) || OS.startsWith(MAC_OS_DARWIN);
    }

    public static String getTargetPlatform() {
        return TychoConstants.ECLIPSE_LATEST;
    }

    public static String getTestSettings() {
        String value = getProperty("its-settings");
        if (value == null || value.contains("$"))
            return null;
        return value;
    }

    public static String getMavenHome() {
        String systemValue = System.getProperty("tychodev-maven.home");
        if (systemValue != null) {
            return systemValue;
        }
        String property = getProperty("maven-dir");
        if (property == null) {
            ProcessBuilder pb = new ProcessBuilder("mvn", "-V");
            pb.redirectErrorStream(true);
            try {
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(MAVEN_HOME_INFO)) {
                        property = line.substring(MAVEN_HOME_INFO.length()).trim();
                    }
                }
            } catch (IOException e) {
            }
        }
        return property;
    }

    public static String getTychoVersion() {
        String property = getProperty("tycho-version");
        if (property == null) {
            try {
                List<String> lines = Files.readAllLines(Path.of("pom.xml"));
                Pattern pattern = Pattern.compile("<version>(.*)</version>");
                for (String line : lines) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return property;
    }

    public static int getHttpServerPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();
            if (localPort > 0) {
                return localPort;
            }
        } catch (IOException e) {
        }
        String port = getProperty("server-port");
        return Integer.parseInt(port);
    }

    public static String getLocalRepo() {
        String property = getProperty("local-repo");
        if (property == null) {
            try {
                return Files.createTempDirectory("tycho_its").toString();
            } catch (IOException e) {
            }
        }
        return property;
    }

}
