/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Andrew Niefer - IBM Corporation - initial API and implementation
 *     Sonatype Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.shared;

import java.util.Properties;

/**
 * Creative copy&paste from org.eclipse.equinox.internal.launcher.Constants and
 * org.eclipse.equinox.launcher.Main.
 * 
 * @author aniefer
 * @author igor
 * 
 */
public class PlatformPropertiesUtils {
    public static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
    public static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
    public static final String INTERNAL_OS_SUNOS = "SunOS"; //$NON-NLS-1$
    public static final String INTERNAL_OS_LINUX = "Linux"; //$NON-NLS-1$
    public static final String INTERNAL_OS_MACOSX = "Mac OS"; //$NON-NLS-1$
    public static final String INTERNAL_OS_AIX = "AIX"; //$NON-NLS-1$
    public static final String INTERNAL_OS_HPUX = "HP-UX"; //$NON-NLS-1$
    public static final String INTERNAL_OS_QNX = "QNX"; //$NON-NLS-1$

    public static final String ARCH_X86 = "x86";//$NON-NLS-1$
    public static final String ARCH_X86_64 = "x86_64";//$NON-NLS-1$

    public final static String OSGI_WS = "osgi.ws"; //$NON-NLS-1$
    public final static String OSGI_OS = "osgi.os"; //$NON-NLS-1$
    public final static String OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
    public final static String OSGI_NL = "osgi.nl"; //$NON-NLS-1$

    private static final class Constants extends PlatformPropertiesUtils {
    }

    /**
     * Constant string (value "win32") indicating the platform is running on a Window 32-bit
     * operating system (e.g., Windows 98, NT, 2000).
     */
    public static final String OS_WIN32 = "win32";//$NON-NLS-1$

    /**
     * Constant string (value "linux") indicating the platform is running on a Linux-based operating
     * system.
     */
    public static final String OS_LINUX = "linux";//$NON-NLS-1$

    /**
     * Constant string (value "aix") indicating the platform is running on an AIX-based operating
     * system.
     */
    public static final String OS_AIX = "aix";//$NON-NLS-1$

    /**
     * Constant string (value "solaris") indicating the platform is running on a Solaris-based
     * operating system.
     */
    public static final String OS_SOLARIS = "solaris";//$NON-NLS-1$

    /**
     * Constant string (value "hpux") indicating the platform is running on an HP/UX-based operating
     * system.
     */
    public static final String OS_HPUX = "hpux";//$NON-NLS-1$

    /**
     * Constant string (value "qnx") indicating the platform is running on a QNX-based operating
     * system.
     */
    public static final String OS_QNX = "qnx";//$NON-NLS-1$

    /**
     * Constant string (value "macosx") indicating the platform is running on a Mac OS X operating
     * system.
     */
    public static final String OS_MACOSX = "macosx";//$NON-NLS-1$

    /**
     * Constant string (value "unknown") indicating the platform is running on a machine running an
     * unknown operating system.
     */
    public static final String OS_UNKNOWN = "unknown";//$NON-NLS-1$

    /**
     * Constant string (value "win32") indicating the platform is running on a machine using the
     * Windows windowing system.
     */
    public static final String WS_WIN32 = "win32";//$NON-NLS-1$

    /**
     * Constant string (value "wpf") indicating the platform is running on a machine using the
     * Windows Presendation Foundation system.
     */
    public static final String WS_WPF = "wpf";//$NON-NLS-1$

    /**
     * Constant string (value "motif") indicating the platform is running on a machine using the
     * Motif windowing system.
     */
    public static final String WS_MOTIF = "motif";//$NON-NLS-1$

    /**
     * Constant string (value "gtk") indicating the platform is running on a machine using the GTK
     * windowing system.
     */
    public static final String WS_GTK = "gtk";//$NON-NLS-1$

    /**
     * Constant string (value "photon") indicating the platform is running on a machine using the
     * Photon windowing system.
     */
    public static final String WS_PHOTON = "photon";//$NON-NLS-1$

    /**
     * Constant string (value "carbon") indicating the platform is running on a machine using the
     * Carbon windowing system (Mac OS X).
     */
    public static final String WS_CARBON = "carbon";//$NON-NLS-1$

    /**
     * Constant string (value "cocoa") indicating the platform is running on a machine using the
     * Carbon windowing system (Mac OS X).
     */
    public static final String WS_COCOA = "cocoa";//$NON-NLS-1$

    /**
     * Constant string (value "unknown") indicating the platform is running on a machine running an
     * unknown windowing system.
     */
    public static final String WS_UNKNOWN = "unknown";//$NON-NLS-1$

    public static String getWS(Properties properties) {
        String ws = properties.getProperty(OSGI_WS);
        if (ws != null)
            return ws;
        String osName = getOS(properties);
        if (osName.equals(Constants.OS_WIN32))
            return Constants.WS_WIN32;
        if (osName.equals(Constants.OS_LINUX))
            return Constants.WS_GTK;
        if (osName.equals(Constants.OS_MACOSX)) {
            String arch = getArch(properties);
            if (ARCH_X86_64.equals(arch))
                return Constants.WS_COCOA;
            return Constants.WS_CARBON;
        }
        if (osName.equals(Constants.OS_HPUX))
            return Constants.WS_MOTIF;
        if (osName.equals(Constants.OS_AIX))
            return Constants.WS_MOTIF;
        if (osName.equals(Constants.OS_SOLARIS))
            return Constants.WS_GTK;
        if (osName.equals(Constants.OS_QNX))
            return Constants.WS_PHOTON;
        return Constants.WS_UNKNOWN;
    }

    public static String getOS(Properties properties) {
        String os = properties.getProperty(OSGI_OS);
        if (os != null)
            return os;

        String osName = System.getProperties().getProperty("os.name"); //$NON-NLS-1$
        if (osName.regionMatches(true, 0, Constants.OS_WIN32, 0, 3))
            return Constants.OS_WIN32;
        // EXCEPTION: All mappings of SunOS convert to Solaris
        if (osName.equalsIgnoreCase(Constants.INTERNAL_OS_SUNOS))
            return Constants.OS_SOLARIS;
        if (osName.equalsIgnoreCase(Constants.INTERNAL_OS_LINUX))
            return Constants.OS_LINUX;
        if (osName.equalsIgnoreCase(Constants.INTERNAL_OS_QNX))
            return Constants.OS_QNX;
        if (osName.equalsIgnoreCase(Constants.INTERNAL_OS_AIX))
            return Constants.OS_AIX;
        if (osName.equalsIgnoreCase(Constants.INTERNAL_OS_HPUX))
            return Constants.OS_HPUX;
        // os.name on Mac OS can be either Mac OS or Mac OS X
        if (osName.regionMatches(true, 0, Constants.INTERNAL_OS_MACOSX, 0, Constants.INTERNAL_OS_MACOSX.length()))
            return Constants.OS_MACOSX;
        return Constants.OS_UNKNOWN;
    }

    public static String getArch(Properties properties) {
        String arch = properties.getProperty(OSGI_ARCH);
        if (arch != null)
            return arch;
        String name = System.getProperties().getProperty("os.arch");//$NON-NLS-1$
        // Map i386 architecture to x86
        if (name.equalsIgnoreCase(Constants.INTERNAL_ARCH_I386))
            return Constants.ARCH_X86;
        // Map amd64 architecture to x86_64
        else if (name.equalsIgnoreCase(Constants.INTERNAL_AMD64))
            return Constants.ARCH_X86_64;

        return name;
    }

}
