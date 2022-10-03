/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.p2.publisher.MavenPropertiesAdvice;
import org.junit.Test;

public class MavenPropertiesAdviceTest {

    @Test
    public void testIUPropertiesNullClassifier() {
        Map<String, String> iuProperties = createIUProperties(null);
        assertNull(iuProperties.get(TychoConstants.PROP_CLASSIFIER));
    }

    @Test
    public void testIUPropertiesEmptyClassifier() {
        Map<String, String> iuProperties = createIUProperties("");
        assertNull(iuProperties.get(TychoConstants.PROP_CLASSIFIER));
    }

    @Test
    public void testIUPropertiesNonEmptyClassifier() {
        Map<String, String> iuProperties = createIUProperties("sources");
        assertEquals("sources", iuProperties.get(TychoConstants.PROP_CLASSIFIER));
    }

    private Map<String, String> createIUProperties(String classifier) {
        MavenPropertiesAdvice mavenPropertiesAdvice = new MavenPropertiesAdvice("groupId", "artifactId", "1.0.0",
                classifier, null, null, null);
        Map<String, String> iuProperties = mavenPropertiesAdvice.getInstallableUnitProperties(null);
        return iuProperties;
    }

}
