/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - additional test cases
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.junit.Test;

public class TargetDefinitionFileTest {
    @Test
    public void testTarget() throws Exception {
        TargetDefinitionFile target = TargetDefinitionFile.read(new File("src/test/resources/modelio/target.target"));

        List<? extends Location> locations = target.getLocations();
        assertEquals(2, locations.size());

        InstallableUnitLocation location = (InstallableUnitLocation) locations.get(0);
        assertEquals(1, location.getRepositories().size());
        assertEquals(URI.create("http://download.eclipse.org/eclipse/updates/3.5/"), location.getRepositories().get(0)
                .getLocation());
        assertEquals(1, location.getUnits().size());
        assertEquals("org.eclipse.platform.sdk", location.getUnits().get(0).getId());
        assertEquals("3.5.2.M20100211-1343", location.getUnits().get(0).getVersion());

        InstallableUnitLocation l02 = (InstallableUnitLocation) locations.get(1);
        assertEquals(5, l02.getUnits().size());
        assertEquals(2, l02.getRepositories().size());
        assertEquals(URI.create("http://subclipse.tigris.org/update_1.6.x/"), l02.getRepositories().get(0)
                .getLocation());
        assertEquals(URI.create("http://download.eclipse.org/tools/mylyn/update/e3.4/"), l02.getRepositories().get(1)
                .getLocation());
    }

    @Test
    public void testLocationTypes() throws Exception {
        TargetDefinitionFile target = TargetDefinitionFile.read(new File(
                "src/test/resources/modelio/locationtypes.target"));

        List<? extends Location> locations = target.getLocations();
        assertEquals("Directory", locations.get(0).getTypeDescription());
        assertEquals("Profile", locations.get(1).getTypeDescription());
        assertEquals("Feature", locations.get(2).getTypeDescription());
        assertEquals("InstallableUnit", locations.get(3).getTypeDescription());

        for (int ix = 0; ix < 3; ix++) {
            assertFalse(locations.get(ix) instanceof InstallableUnitLocation);
        }
        assertTrue(locations.get(3) instanceof InstallableUnitLocation);
    }
}
