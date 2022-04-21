/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.model.BundleConfiguration;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.FeatureRef.InstallMode;
import org.eclipse.tycho.model.Launcher;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.ProductConfiguration;
import org.junit.jupiter.api.Test;

class ProductConfigurationTest {

    @Test
    void testProductConfigurationParse() throws Exception {
        ProductConfiguration config = ProductConfiguration
                .read(getClass().getResourceAsStream("/product/MyFirstRCP.product"));

        assertEquals("My First RCP", config.getName());
        assertEquals("MyFirstRCP.product1", config.getProduct());
        assertEquals("MyFirstRCP.application", config.getApplication());
        assertEquals(false, config.useFeatures());

        /*
         * ConfigIni configIni = config.getConfigIni();assertNotNull(configIni);
         * assertEquals("linux.ini", configIni.getLinuxIcon()); assertEquals("macosx.ini",
         * configIni.getMacosxIcon()); assertEquals("solaris.ini", configIni.getSolarisIcon());
         * assertEquals("win32.ini", configIni.getWin32());
         * 
         * LauncherArguments launcherArgs = config.getLauncherArgs();
         * assertNotNull(launcherArgs);assertEquals("-all args",
         * launcherArgs.getProgramArgs());assertEquals("-linux args",
         * launcherArgs.getProgramArgsLin());assertEquals("-mac args",
         * launcherArgs.getProgramArgsMac());assertEquals("-solaris args",
         * launcherArgs.getProgramArgsSol());assertEquals("-win32 args",
         * launcherArgs.getProgramArgsWin());assertEquals("-all vm",
         * launcherArgs.getVmArgs());assertEquals("-linux vm", launcherArgs.getVmArgsLin());
         * assertEquals("-mac vm", launcherArgs.getVmArgsMac()); assertEquals("-solaris vm",
         * launcherArgs.getVmArgsSol()); assertEquals("-win32 vm", launcherArgs.getVmArgsWin());
         */

        Launcher launcher = config.getLauncher();
        assertNotNull(launcher);
        assertEquals("launchername", launcher.getName());
        assertEquals("XPM", launcher.getLinuxIcon().get(Launcher.ICON_LINUX));
        assertEquals("XPM", launcher.getFreeBSDIcon().get(Launcher.ICON_FREEBSD));
        assertEquals("icns", launcher.getMacosxIcon().get(Launcher.ICON_MAC));
        assertEquals("large", launcher.getSolarisIcon().get(Launcher.ICON_SOLARIS_LARGE));
        assertEquals("medium", launcher.getSolarisIcon().get(Launcher.ICON_SOLARIS_MEDIUM));
        assertEquals("small", launcher.getSolarisIcon().get(Launcher.ICON_SOLARIS_SMALL));
        assertEquals("tiny", launcher.getSolarisIcon().get(Launcher.ICON_SOLARIS_TINY));
        assertEquals(false, launcher.getWindowsUseIco());
//	assertEquals("iconon", launcher.getWindowsIcon().getIco().getPath());
        assertEquals("16-32", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_SMALL_HIGH));
        assertEquals("16-8", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_SMALL_LOW));
        assertEquals("32-32", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_MEDIUM_HIGH));
        assertEquals("32-8", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_MEDIUM_LOW));
        assertEquals("48-32", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_LARGE_HIGH));
        assertEquals("48-8", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_LARGE_LOW));
        assertEquals("256-32", launcher.getWindowsIcon().get(Launcher.ICON_WINDOWS_EXTRA_LARGE_HIGH));

        List<PluginRef> plugins = config.getPlugins();
        assertNotNull(plugins);
        assertEquals(2, plugins.size());

        PluginRef plugin = plugins.get(0);
        assertNotNull(plugin);
        assertEquals("HeadlessProduct", plugin.getId());
        assertNull(plugin.getVersion());

        List<FeatureRef> features = config.getFeatures();
        assertNotNull(features);
        assertEquals(2, features.size());

        FeatureRef feature = features.get(0);
        assertNotNull(feature);
        assertEquals("HeadlessFeature", feature.getId());
        assertEquals("1.0.0", feature.getVersion());
    }

    @Test
    void testProductConfigurationParseWithStartLevel() throws Exception {
        ProductConfiguration config = ProductConfiguration
                .read(getClass().getResourceAsStream("/product/MyProduct.product"));
        Map<String, BundleConfiguration> bundles = config.getPluginConfiguration();
//		<plugin id="org.eclipse.core.contenttype" autoStart="true" startLevel="1" />
        BundleConfiguration contentType = bundles.get("org.eclipse.core.contenttype");
        assertNotNull(contentType);
        assertTrue(contentType.isAutoStart());
        assertEquals(1, contentType.getStartLevel());

//	      <plugin id="HeadlessProduct" autoStart="false" startLevel="2" />
        BundleConfiguration headlessProduct = bundles.get("HeadlessProduct");
        assertNotNull(headlessProduct);
        assertFalse(headlessProduct.isAutoStart());
        assertEquals(2, headlessProduct.getStartLevel());

    }

    @Test
    void testFeatureInstallMode() throws Exception {
        ProductConfiguration config = ProductConfiguration
                .read(getClass().getResourceAsStream("/product/rootFeatures.product"));

        Map<String, InstallMode> modes = getInstallModes(config);

        assertEquals(InstallMode.include, modes.get("org.eclipse.rcp"));
        assertEquals(InstallMode.include, modes.get("org.eclipse.e4.rcp"));
        assertEquals(InstallMode.root, modes.get("org.eclipse.help"));
        assertEquals(InstallMode.root, modes.get("org.eclipse.egit"));
        assertEquals(4, modes.size());
    }

    @Test
    void testRemoveRootFeatures() throws Exception {
        ProductConfiguration config = ProductConfiguration
                .read(getClass().getResourceAsStream("/product/rootFeatures.product"));

        config.removeRootInstalledFeatures();

        Map<String, InstallMode> modes = getInstallModes(config);

        assertEquals(InstallMode.include, modes.get("org.eclipse.rcp"));
        assertEquals(InstallMode.include, modes.get("org.eclipse.e4.rcp"));
        assertEquals(2, modes.size());
    }

    private static Map<String, InstallMode> getInstallModes(ProductConfiguration config) {
        Map<String, InstallMode> modes = new HashMap<>();
        for (FeatureRef featureRef : config.getFeatures()) {
            modes.put(featureRef.getId(), featureRef.getInstallMode());
        }
        return modes;
    }
}
