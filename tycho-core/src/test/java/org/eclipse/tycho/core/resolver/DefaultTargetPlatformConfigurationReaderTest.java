/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration.InjectP2MavenMetadataHandling;
import org.junit.Test;

public class DefaultTargetPlatformConfigurationReaderTest {

    private final DefaultTargetPlatformConfigurationReader reader = new DefaultTargetPlatformConfigurationReader();

    @Test
    public void defaultsToValidateWhenUnset() {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();
        reader.setP2MavenMetadataHandling(result, new Xpp3Dom("configuration"));
        assertEquals(InjectP2MavenMetadataHandling.validate, result.getP2MetadataHandling());
    }

    @Test
    public void readsInject() {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();
        reader.setP2MavenMetadataHandling(result, config("inject"));
        assertEquals(InjectP2MavenMetadataHandling.inject, result.getP2MetadataHandling());
    }

    @Test
    public void readsIgnore() {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();
        reader.setP2MavenMetadataHandling(result, config("ignore"));
        assertEquals(InjectP2MavenMetadataHandling.ignore, result.getP2MetadataHandling());
    }

    @Test
    public void rejectsInvalidValue() {
        assertThrows(BuildFailureException.class,
                () -> reader.setP2MavenMetadataHandling(new TargetPlatformConfiguration(), config("bogus")));
    }

    private static Xpp3Dom config(String value) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom child = new Xpp3Dom("p2MavenMetadataHandling");
        child.setValue(value);
        configuration.addChild(child);
        return configuration;
    }
}
