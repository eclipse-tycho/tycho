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
package org.eclipse.tycho.core.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.tycho.core.osgitools.targetplatform.EclipseInstallationLayout;

public class EclipseInstallationLayoutTest extends PlexusTestCase {

    public void testTargetPlatform() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/wtp-2.0").getCanonicalFile();
        EclipseInstallationLayout finder = getPluginFinder(targetPlatform);

        List<File> sites = getCannonicalFiles(finder.getSites());

        assertEquals(4, sites.size());
        assertTrue(sites.toString(), sites.contains(targetPlatform));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/zest-3.4")));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "../subclipse-1.3").getCanonicalFile()));
    }

    private EclipseInstallationLayout getPluginFinder(File location) throws Exception {
        EclipseInstallationLayout layout = lookup(EclipseInstallationLayout.class);
        layout.setLocation(location);
        return layout;
    }

    public void testPlugins33() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/wtp-2.0").getCanonicalFile();
        EclipseInstallationLayout finder = getPluginFinder(targetPlatform);

        List<File> plugins = getCannonicalFiles(finder.getPlugins(targetPlatform));

        assertEquals(2, plugins.size());
        assertTrue(plugins.contains(new File(targetPlatform, "plugins/com.ibm.icu.source_3.6.1.v20070906")
                .getCanonicalFile()));
        assertTrue(plugins.contains(new File(targetPlatform,
                "plugins/org.eclipse.datatools.enablement.sybase.asa.models_1.0.0.200706071.jar").getCanonicalFile()));
    }

    public void testPlugins34() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/wtp-3.0").getCanonicalFile();
        EclipseInstallationLayout finder = getPluginFinder(targetPlatform);

        List<File> plugins = new ArrayList<File>();
        for (File site : finder.getSites()) {
            plugins.addAll(getCannonicalFiles(finder.getPlugins(site)));
        }

        assertEquals(2, plugins.size());
//		assertTrue(plugins.contains(new File(targetPlatform, "plugins/com.ibm.icu_3.8.1.v20080402.jar").getCanonicalFile()));
//		assertTrue(plugins.contains(new File(targetPlatform, "plugins/org.junit4_4.3.1").getCanonicalFile()));
        assertTrue(plugins.contains(new File(targetPlatform, "dropins/com.ibm.icu.source_3.6.1.v20070906")
                .getCanonicalFile()));
        assertTrue(plugins.contains(new File(targetPlatform,
                "dropins/org.eclipse.datatools.enablement.sybase.asa.models_1.0.0.200706071.jar").getCanonicalFile()));
    }

    public void testSites34() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/wtp-3.0").getCanonicalFile();
        EclipseInstallationLayout finder = getPluginFinder(targetPlatform);

        List<File> sites = getCannonicalFiles(finder.getSites());

        assertEquals(6, sites.size());
        assertTrue(sites.toString(), sites.contains(targetPlatform));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/ajdt")));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/eclipse")));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "dropins/emf/eclipse")));
        assertTrue(sites.toString(), sites.contains(new File(targetPlatform, "../subclipse-1.3").getCanonicalFile()));
    }

    private List<File> getCannonicalFiles(Set<File> files) throws IOException {
        ArrayList<File> result = new ArrayList<File>();
        for (File file : files) {
            result.add(file.getCanonicalFile());
        }
        return result;
    }

    public void testSitesSimple() throws Exception {
        File targetPlatform = new File("src/test/resources/targetplatforms/simple").getCanonicalFile();
        EclipseInstallationLayout finder = getPluginFinder(targetPlatform);

        List<File> sites = new ArrayList<File>(finder.getSites());

        assertEquals(2, sites.size());
        assertEquals(targetPlatform, sites.get(0));
    }
}
