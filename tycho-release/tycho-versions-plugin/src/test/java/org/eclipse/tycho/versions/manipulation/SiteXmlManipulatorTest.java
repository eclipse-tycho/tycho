/*******************************************************************************
 * Copyright (c) 2011, 2018 Inventage AG and others..
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
        assertEquals(expectedUrl,
                SiteXmlManipulator.rewriteFeatureUrl(oldUrl, new PomVersionChange(null, oldVersion, "NEW")));
    }

}
