/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.p2maven.InstallableUnitProvider;
import org.eclipse.tycho.p2resolver.P2ResolverImpl;

/**
 * This provides P2 visible meta-data for bundles that are not expressed in the manifest (e.g.
 * build.properties derived)
 *
 */
@Component(role = InstallableUnitProvider.class, hint = "eclipse-test-plugin-packaging")
public class TestPluginPackagingInstallableUnitProvider implements InstallableUnitProvider {
    @Requirement
    private Logger logger;
    private Collection<IInstallableUnit> eclipseTestPackagingIUs;

    public TestPluginPackagingInstallableUnitProvider() {
        List<IRequirement> list = new ArrayList<>();
        P2ResolverImpl.addDependenciesForTests(list::add);
        eclipseTestPackagingIUs = createIU(list);
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        if (PackagingType.TYPE_ECLIPSE_TEST_PLUGIN.equals(project.getPackaging())) {
            return eclipseTestPackagingIUs;
        }
        return Collections.emptyList();
    }

    private Collection<IInstallableUnit> createIU(Collection<IRequirement> additionalBundleRequirements) {
        if (additionalBundleRequirements.isEmpty()) {
            return Collections.emptyList();
        }
        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        result.setId("eclipse-test-plugin-packaging-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
        result.addRequirements(additionalBundleRequirements);
        return List.of(MetadataFactory.createInstallableUnit(result));
    }

}
