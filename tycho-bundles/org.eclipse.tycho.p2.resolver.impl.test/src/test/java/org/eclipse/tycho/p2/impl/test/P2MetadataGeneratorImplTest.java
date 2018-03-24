/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.P2GeneratorImpl;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.test.util.BuildPropertiesParserForTesting;
import org.junit.Assert;
import org.junit.Test;

public class P2MetadataGeneratorImplTest {
    @Test
    public void gav() throws Exception {
        P2GeneratorImpl impl = new P2GeneratorImpl(false);
        impl.setBuildPropertiesParser(new BuildPropertiesParserForTesting());
        File location = new File("resources/generator/bundle").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.test";
        String artifactId = "bundle";
        String version = "1.0.0-SNAPSHOT";
        List<TargetEnvironment> environments = new ArrayList<>();
        DependencyMetadata metadata = impl.generateMetadata(
                new ArtifactMock(location, groupId, artifactId, version, PackagingType.TYPE_ECLIPSE_PLUGIN),
                environments);

        List<IInstallableUnit> units = new ArrayList<>(metadata.getInstallableUnits());
        List<IArtifactDescriptor> artifacts = new ArrayList<>(metadata.getArtifactDescriptors());

        Assert.assertEquals(1, units.size());
        IInstallableUnit unit = units.iterator().next();

        Assert.assertEquals("org.eclipse.tycho.p2.impl.test.bundle", unit.getId());
        Assert.assertEquals("1.0.0.qualifier", unit.getVersion().toString());
        Assert.assertEquals(3, unit.getRequirements().size());

        Assert.assertEquals(1, artifacts.size());
        IArtifactDescriptor ad = artifacts.iterator().next();
        Assert.assertEquals("org.eclipse.tycho.p2.impl.test.bundle", ad.getArtifactKey().getId());
        Assert.assertEquals("1.0.0.qualifier", ad.getArtifactKey().getVersion().toString());

        Assert.assertEquals(groupId, ad.getProperties().get(RepositoryLayoutHelper.PROP_GROUP_ID));
        Assert.assertEquals(artifactId, ad.getProperties().get(RepositoryLayoutHelper.PROP_ARTIFACT_ID));
        Assert.assertEquals(version, ad.getProperties().get(RepositoryLayoutHelper.PROP_VERSION));
    }

}
