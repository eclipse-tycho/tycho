/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP SE - additional test cases
 *    Christoph Läubrich - Adjust to new API
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.tycho.core.ee.TargetDefinitionFile;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.DirectoryLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.FeaturesLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.IncludeMode;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.ProfileLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinitionSyntaxException;
import org.junit.jupiter.api.Test;

public class TargetDefinitionFileTest {

    @Test
    public void testTarget() throws Exception {
        List<? extends Location> locations = readTarget("target.target").getLocations();
        assertEquals(2, locations.size());

        InstallableUnitLocation location = (InstallableUnitLocation) locations.get(0);
        assertEquals(1, location.getRepositories().size());
        assertEquals(URI.create("https://download.eclipse.org/eclipse/updates/3.5/"),
                location.getRepositories().get(0).getLocation());
        assertEquals(1, location.getUnits().size());
        assertEquals("org.eclipse.platform.sdk", location.getUnits().get(0).getId());
        assertEquals("3.5.2.M20100211-1343", location.getUnits().get(0).getVersion());

        InstallableUnitLocation l02 = (InstallableUnitLocation) locations.get(1);
        assertEquals(5, l02.getUnits().size());
        assertEquals(2, l02.getRepositories().size());
        assertEquals(URI.create("http://subclipse.tigris.org/update_1.6.x/"),
                l02.getRepositories().get(0).getLocation());
        assertEquals(URI.create("https://download.eclipse.org/tools/mylyn/update/e3.4/"),
                l02.getRepositories().get(1).getLocation());
    }

    @Test
    public void testLocationTypes() throws Exception {
        List<? extends Location> locations = readTarget("locationtypes.target").getLocations();
        assertEquals("Directory", locations.get(0).getTypeDescription());
        assertEquals("Profile", locations.get(1).getTypeDescription());
        assertEquals("Feature", locations.get(2).getTypeDescription());
        assertEquals("InstallableUnit", locations.get(3).getTypeDescription());
        assertTrue(locations.get(0) instanceof DirectoryLocation);
        assertTrue(locations.get(1) instanceof ProfileLocation);
        assertTrue(locations.get(2) instanceof FeaturesLocation);
        assertTrue(locations.get(3) instanceof InstallableUnitLocation);
    }

    @Test
    public void testDefaultIncludeModeValues() throws Exception {
        List<? extends Location> locations = readTarget("includeModes.target").getLocations();
        InstallableUnitLocation locationWithDefaults = (InstallableUnitLocation) locations.get(0);
        assertEquals(IncludeMode.PLANNER, locationWithDefaults.getIncludeMode());
        assertEquals(false, locationWithDefaults.includeAllEnvironments());
    }

    @Test
    public void testExplictIncludeModeValues() throws Exception {
        List<? extends Location> locations = readTarget("includeModes.target").getLocations();
        InstallableUnitLocation locationWithPlanner = (InstallableUnitLocation) locations.get(1);
        InstallableUnitLocation locationWithSlicer = (InstallableUnitLocation) locations.get(2);
        InstallableUnitLocation locationWithSlicerAndAllEnvironments = (InstallableUnitLocation) locations.get(3);
        assertEquals(IncludeMode.PLANNER, locationWithPlanner.getIncludeMode());
        assertEquals(IncludeMode.SLICER, locationWithSlicer.getIncludeMode());
        assertEquals(false, locationWithSlicer.includeAllEnvironments());
        assertEquals(IncludeMode.SLICER, locationWithSlicerAndAllEnvironments.getIncludeMode());
        assertEquals(true, locationWithSlicerAndAllEnvironments.includeAllEnvironments());
    }

    @Test
    public void testIncludeSource() throws Exception {
        List<? extends Location> locations = readTarget("includeSource.target", IncludeSourceMode.honor).getLocations();
        InstallableUnitLocation locationWithSources = (InstallableUnitLocation) locations.get(0);
        InstallableUnitLocation locationWithoutSources = (InstallableUnitLocation) locations.get(1);
        InstallableUnitLocation locationWithoutIncludeSourceAttribute = (InstallableUnitLocation) locations.get(2);
        assertEquals(true, locationWithSources.includeSource());
        assertEquals(false, locationWithoutSources.includeSource());
        assertEquals(false, locationWithoutIncludeSourceAttribute.includeSource());
    }

    @Test
    public void testInvalidXML() throws Exception {
        try {
            readTarget("invalidXML.target").getLocations();
        } catch (RuntimeException e) {
            assertEquals(TargetDefinitionSyntaxException.class, e.getCause().getClass());
        }
    }

    public void testInvalidIncludeMode() throws Exception {

        List<? extends Location> locations = readTarget("invalidMode.target").getLocations();

        // allow exception to be thrown late
        InstallableUnitLocation invalidIncludeModeLocation = (InstallableUnitLocation) locations.get(0);
        assertThrows(TargetDefinitionSyntaxException.class, () -> invalidIncludeModeLocation.getIncludeMode());
    }

    @Test
    public void testBundleSelectionList() throws Exception {
        TargetDefinitionFile targetFile = readTarget("withBundleSelection.target");
        assertTrue(targetFile.hasIncludedBundles());
    }

    @Test
    public void testNoBundleSelectionList() throws Exception {
        TargetDefinitionFile targetFile = readTarget("target.target");
        assertFalse(targetFile.hasIncludedBundles());
    }

    private TargetDefinitionFile readTarget(String fileName) throws IOException {
        return readTarget(fileName, IncludeSourceMode.honor);
    }

    private TargetDefinitionFile readTarget(String fileName, IncludeSourceMode includeSourceMode) throws IOException {
        return TargetDefinitionFile.read(new File("src/test/resources/modelio/" + fileName));
    }

}
