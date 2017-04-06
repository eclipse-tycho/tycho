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

import static org.junit.Assert.assertEquals;

import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.junit.Test;

public class SiteXmlManipulatorTest {

    @Test
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
        new SiteXmlManipulator();
        assertEquals(expectedUrl,
                SiteXmlManipulator.rewriteFeatureUrl(oldUrl, new PomVersionChange(null, oldVersion, "NEW")));
    }

}
