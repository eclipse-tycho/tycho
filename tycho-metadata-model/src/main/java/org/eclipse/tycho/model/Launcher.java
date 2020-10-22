/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.pdark.decentxml.Element;

public class Launcher {

    public static final String ICON_LINUX = "icon";

    public static final String ICON_MAC = ICON_LINUX;

    public static final String ICON_FREEBSD = ICON_LINUX;

    public static final String ICON_WINDOWS_ICO_PATH = "path";

    public static final String ICON_WINDOWS_EXTRA_LARGE_HIGH = "winExtraLargeHigh";

    public static final String ICON_WINDOWS_LARGE_LOW = "winLargeLow";

    public static final String ICON_WINDOWS_LARGE_HIGH = "winLargeHigh";

    public static final String ICON_WINDOWS_MEDIUM_LOW = "winMediumLow";

    public static final String ICON_WINDOWS_MEDIUM_HIGH = "winMediumHigh";

    public static final String ICON_WINDOWS_SMALL_LOW = "winSmallLow";

    public static final String ICON_WINDOWS_SMALL_HIGH = "winSmallHigh";

    public static final String ICON_SOLARIS_TINY = "solarisTiny";

    public static final String ICON_SOLARIS_SMALL = "solarisSmall";

    public static final String ICON_SOLARIS_MEDIUM = "solarisMedium";

    public static final String ICON_SOLARIS_LARGE = "solarisLarge";

    private Element dom;

    public Launcher(Element domLauncher) {
        this.dom = domLauncher;
    }

    public String getName() {
        return dom.getAttributeValue("name");
    }

    public Map<String, String> getLinuxIcon() {
        Element linuxDom = dom.getChild("linux");
        if (linuxDom == null) {
            return Collections.emptyMap();
        }
        Map<String, String> linux = new HashMap<>();
        putIfNotNull(linux, ICON_LINUX, linuxDom.getAttributeValue(ICON_LINUX));
        return Collections.unmodifiableMap(linux);
    }

    public Map<String, String> getFreeBSDIcon() {
        Element freebsdDom = dom.getChild("freebsd");
        if (freebsdDom == null) {
            return Collections.emptyMap();
        }
        Map<String, String> freebsd = new HashMap<>(1);
        putIfNotNull(freebsd, ICON_FREEBSD, freebsdDom.getAttributeValue(ICON_FREEBSD));
        return Collections.unmodifiableMap(freebsd);
    }

    public Map<String, String> getMacosxIcon() {
        Element macosxDom = dom.getChild("macosx");
        if (macosxDom == null) {
            return Collections.emptyMap();
        }
        Map<String, String> mac = new HashMap<>();
        putIfNotNull(mac, ICON_LINUX, macosxDom.getAttributeValue(ICON_LINUX));
        return Collections.unmodifiableMap(mac);
    }

    public Map<String, String> getSolarisIcon() {
        Element solarisDom = dom.getChild("solaris");
        if (solarisDom == null) {
            return Collections.emptyMap();
        }
        Map<String, String> solaris = new HashMap<>();
        putIfNotNull(solaris, ICON_SOLARIS_LARGE, solarisDom.getAttributeValue(ICON_SOLARIS_LARGE));
        putIfNotNull(solaris, ICON_SOLARIS_MEDIUM, solarisDom.getAttributeValue(ICON_SOLARIS_MEDIUM));
        putIfNotNull(solaris, ICON_SOLARIS_SMALL, solarisDom.getAttributeValue(ICON_SOLARIS_SMALL));
        putIfNotNull(solaris, ICON_SOLARIS_TINY, solarisDom.getAttributeValue(ICON_SOLARIS_TINY));
        return Collections.unmodifiableMap(solaris);
    }

    public boolean getWindowsUseIco() {
        Element winDom = dom.getChild("win");
        if (winDom == null) {
            return false;
        }
        boolean useIco = Boolean.parseBoolean(winDom.getAttributeValue("useIco"));
        return useIco;
    }

    public Map<String, String> getWindowsIcon() {
        Element winDom = dom.getChild("win");
        if (winDom == null) {
            return Collections.emptyMap();
        }
        Map<String, String> windows = new HashMap<>();
        if (getWindowsUseIco()) {
            Element ico = winDom.getChild("ico");
            if (ico != null) {
                putIfNotNull(windows, ICON_WINDOWS_ICO_PATH, ico.getAttributeValue(ICON_WINDOWS_ICO_PATH));
            }
        } else {
            Element bmp = winDom.getChild("bmp");
            if (bmp != null) {
                putIfNotNull(windows, ICON_WINDOWS_SMALL_HIGH, bmp.getAttributeValue(ICON_WINDOWS_SMALL_HIGH));
                putIfNotNull(windows, ICON_WINDOWS_SMALL_LOW, bmp.getAttributeValue(ICON_WINDOWS_SMALL_LOW));
                putIfNotNull(windows, ICON_WINDOWS_MEDIUM_HIGH, bmp.getAttributeValue(ICON_WINDOWS_MEDIUM_HIGH));
                putIfNotNull(windows, ICON_WINDOWS_MEDIUM_LOW, bmp.getAttributeValue(ICON_WINDOWS_MEDIUM_LOW));
                putIfNotNull(windows, ICON_WINDOWS_LARGE_HIGH, bmp.getAttributeValue(ICON_WINDOWS_LARGE_HIGH));
                putIfNotNull(windows, ICON_WINDOWS_LARGE_LOW, bmp.getAttributeValue(ICON_WINDOWS_LARGE_LOW));
                putIfNotNull(windows, ICON_WINDOWS_EXTRA_LARGE_HIGH,
                        bmp.getAttributeValue(ICON_WINDOWS_EXTRA_LARGE_HIGH));
            }
        }
        return Collections.unmodifiableMap(windows);
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

}
