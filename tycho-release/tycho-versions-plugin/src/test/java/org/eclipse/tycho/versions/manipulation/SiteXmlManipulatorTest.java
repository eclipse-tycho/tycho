/*******************************************************************************
 * Copyright (c) 2011 Inventage AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Beat Strasser (Inventage AG) - preserve feature url in site.xml
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import junit.framework.TestCase;

import org.eclipse.tycho.versions.engine.VersionChange;

public class SiteXmlManipulatorTest extends TestCase {

    public void testFeatureUrlRewriting() {
        assertFeatureUrlRewriting("features/id_NEW.jar", "features/id_1.2.3.jar", "1.2.3");
        assertFeatureUrlRewriting("features/id_NEW.jar", "features/id_1.2.3.qualifier.jar", "1.2.3.qualifier");
        assertFeatureUrlRewriting("features/id_NEW.jar", "features/id_1.2.3.201009091500.jar", "1.2.3.201009091500");
        assertFeatureUrlRewriting("features/id_1x2x3.jar", "features/id_1x2x3.jar", "1.2.3");
        assertFeatureUrlRewriting("features/id-NEW.jar", "features/id-1.2.3.jar", "1.2.3");
        assertFeatureUrlRewriting("id/NEW/id-NEW.jar", "id/1.2.3/id-1.2.3.jar", "1.2.3");
        assertFeatureUrlRewriting("id_NEW.jar", "id_1.2.3.jar", "1.2.3");
        assertFeatureUrlRewriting("id_NEW", "id_1.2.3", "1.2.3");
    }

    private void assertFeatureUrlRewriting(String expectedUrl, String oldUrl, String oldVersion) {
        assertEquals(expectedUrl,
                new SiteXmlManipulator().rewriteFeatureUrl(oldUrl, new VersionChange(null, oldVersion, "NEW")));
    }

}
