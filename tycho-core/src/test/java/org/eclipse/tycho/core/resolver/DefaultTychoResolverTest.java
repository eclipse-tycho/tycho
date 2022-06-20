/*******************************************************************************
 * Copyright (c) 2014, 2015 Bachmann electronic GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronic GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultTychoResolverTest {

    private DefaultTychoResolver defaultTychoResolver;

    @Before
    public void setup() {
        defaultTychoResolver = new DefaultTychoResolver();

    }

    @Test
    public void testTychoEnvironmentPropertiesGetSet() {
        System.setProperty("os.name", PlatformPropertiesUtils.INTERNAL_OS_LINUX);
        System.setProperty("os.arch", PlatformPropertiesUtils.INTERNAL_AMD64);

        MavenProject project = mock(MavenProject.class);
        Properties mergedProperties = new Properties();
        Properties projectProperties = new Properties();
        when(project.getProperties()).thenReturn(projectProperties);

        defaultTychoResolver.setTychoEnvironmentProperties(mergedProperties, project);

        Object ws = projectProperties.getProperty(DefaultTychoResolver.TYCHO_ENV_OSGI_WS);
        Object os = projectProperties.getProperty(DefaultTychoResolver.TYCHO_ENV_OSGI_OS);
        Object arch = projectProperties.getProperty(DefaultTychoResolver.TYCHO_ENV_OSGI_ARCH);

        Assert.assertEquals(3, projectProperties.size());
        Assert.assertEquals(PlatformPropertiesUtils.ARCH_X86_64, arch);
        Assert.assertEquals(PlatformPropertiesUtils.OS_LINUX, os);
        Assert.assertEquals(PlatformPropertiesUtils.WS_GTK, ws);
    }
}
