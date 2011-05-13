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
package org.eclipse.tycho.p2.publisher;

import static org.eclipse.tycho.p2.test.matcher.InstallableUnitMatchers.hasGAV;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ITouchpointData;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.junit.Test;

public class P2GeneratorImplTest {

    @Test
    public void testGenerateSourceBundleMetadata() throws Exception {
        DependencyMetadataGenerator p2GeneratorImpl = new SourcesBundleDependencyMetadataGenerator();
        File location = new File("resources/generator/bundle").getCanonicalFile();
        ArtifactMock artifactMock = new ArtifactMock(location, "org.acme", "foo", "0.0.1", "eclipse-plugin");
        Set<Object> units = p2GeneratorImpl.generateMetadata(artifactMock, getEnvironments());
        assertEquals(1, units.size());
        IInstallableUnit sourceBundleUnit = getUnit("foo.source", units);
        assertNotNull(sourceBundleUnit);
        assertEquals(Version.create("0.0.1"), sourceBundleUnit.getVersion());
        assertThat(sourceBundleUnit, hasGAV("org.acme", "foo", "0.0.1", "sources"));
        ITouchpointData touchPointData = sourceBundleUnit.getTouchpointData().iterator().next();
        String manifestContent = touchPointData.getInstruction("manifest").getBody();
        Manifest manifest = new Manifest(new ByteArrayInputStream(manifestContent.getBytes("UTF-8")));
        Attributes attributes = manifest.getMainAttributes();
        assertEquals("foo.source", attributes.getValue("Bundle-SymbolicName"));
        assertEquals("foo;version=0.0.1;roots:=\".\"", attributes.getValue("Eclipse-SourceBundle"));
    }

    private IInstallableUnit getUnit(String id, Set<Object> units) {
        for (Object obj : units) {
            IInstallableUnit unit = (IInstallableUnit) obj;
            if (id.equals(unit.getId())) {
                return unit;
            }
        }
        return null;
    }

    private List<Map<String, String>> getEnvironments() {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("osgi.os", "linux");
        properties.put("osgi.ws", "gtk");
        properties.put("osgi.arch", "x86_64");

        environments.add(properties);

        return environments;
    }

}
