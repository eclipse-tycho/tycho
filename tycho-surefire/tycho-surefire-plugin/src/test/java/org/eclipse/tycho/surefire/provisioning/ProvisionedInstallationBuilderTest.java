/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.provisioning;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.TargetEnvironment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProvisionedInstallationBuilderTest {

    private static final TargetEnvironment ENV_LINUX = new TargetEnvironment(PlatformPropertiesUtils.OS_LINUX,
            PlatformPropertiesUtils.WS_GTK, PlatformPropertiesUtils.ARCH_X86_64);
    private static final TargetEnvironment ENV_MACOS = new TargetEnvironment(PlatformPropertiesUtils.OS_MACOSX,
            PlatformPropertiesUtils.WS_COCOA, PlatformPropertiesUtils.ARCH_X86_64);

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void setDestination_LayoutNormal() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = tempDir.newFolder("work");
        builder.setTargetEnvironment(ENV_LINUX);
        builder.setDestination(work);
        assertEquals(work, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_NoAppBundleGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = tempDir.newFolder("work");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        File destinationExpected = new File(work, "Eclipse.app/Contents/Eclipse");
        assertEquals(destinationExpected, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_AppBundleRootGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = tempDir.newFolder("work.app");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        File destinationExpected = new File(work, "Contents/Eclipse");
        assertEquals(destinationExpected, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_InstallAreaInsideAppBundleGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = tempDir.newFolder("work.app/Contents/Eclipse");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        assertEquals(work, builder.getEffectiveDestination());
    }

}
