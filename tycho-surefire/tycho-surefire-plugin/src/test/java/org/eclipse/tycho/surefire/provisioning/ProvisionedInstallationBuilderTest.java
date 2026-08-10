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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.TargetEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProvisionedInstallationBuilderTest {

    private static final TargetEnvironment ENV_LINUX = new TargetEnvironment(PlatformPropertiesUtils.OS_LINUX,
            PlatformPropertiesUtils.WS_GTK, PlatformPropertiesUtils.ARCH_X86_64);
    private static final TargetEnvironment ENV_MACOS = new TargetEnvironment(PlatformPropertiesUtils.OS_MACOSX,
            PlatformPropertiesUtils.WS_COCOA, PlatformPropertiesUtils.ARCH_X86_64);

    @TempDir
    Path tempDir;

    @Test
    public void setDestination_LayoutNormal() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = newFolder("work");
        builder.setTargetEnvironment(ENV_LINUX);
        builder.setDestination(work);
        assertEquals(work, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_NoAppBundleGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = newFolder("work");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        File destinationExpected = new File(work, "Eclipse.app/Contents/Eclipse");
        assertEquals(destinationExpected, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_AppBundleRootGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = newFolder("work.app");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        File destinationExpected = new File(work, "Contents/Eclipse");
        assertEquals(destinationExpected, builder.getEffectiveDestination());
    }

    @Test
    public void setDestination_LayoutMacOS_InstallAreaInsideAppBundleGiven() throws Exception {
        ProvisionedInstallationBuilder builder = new ProvisionedInstallationBuilder(null, null);

        File work = newFolder("work.app/Contents/Eclipse");
        builder.setTargetEnvironment(ENV_MACOS);
        builder.setDestination(work);
        assertEquals(work, builder.getEffectiveDestination());
    }

    private File newFolder(String path) throws IOException {
        return Files.createDirectories(tempDir.resolve(path)).toFile();
    }

}
