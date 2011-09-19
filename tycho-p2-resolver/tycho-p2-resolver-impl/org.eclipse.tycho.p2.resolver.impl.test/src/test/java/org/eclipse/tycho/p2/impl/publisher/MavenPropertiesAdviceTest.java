/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import static org.junit.Assert.*;

import java.util.Map;

import org.eclipse.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.junit.Test;

public class MavenPropertiesAdviceTest {

    @Test
    public void testIUPropertiesNullClassifier() {
        Map<String, String> iuProperties = createIUProperties(null);
        assertNull(iuProperties.get(RepositoryLayoutHelper.PROP_CLASSIFIER));
    }

    @Test
    public void testIUPropertiesEmptyClassifier() {
        Map<String, String> iuProperties = createIUProperties("");
        assertNull(iuProperties.get(RepositoryLayoutHelper.PROP_CLASSIFIER));
    }

    @Test
    public void testIUPropertiesNonEmptyClassifier() {
        Map<String, String> iuProperties = createIUProperties("sources");
        assertEquals("sources", iuProperties.get(RepositoryLayoutHelper.PROP_CLASSIFIER));
    }

    private Map<String, String> createIUProperties(String classifier) {
        MavenPropertiesAdvice mavenPropertiesAdvice = new MavenPropertiesAdvice("groupId", "artifactId", "1.0.0",
                classifier);
        Map<String, String> iuProperties = mavenPropertiesAdvice.getInstallableUnitProperties(null);
        return iuProperties;
    }

}
