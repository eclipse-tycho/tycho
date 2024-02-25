/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.osgitools.EclipseRepositoryProject;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.p2maven.actions.ProductFile2;
import org.eclipse.tycho.resolver.InstallableUnitProvider;

@Component(role = InstallableUnitProvider.class, hint = PublishProductInstallableUnitProvider.HINT)
public class PublishProductInstallableUnitProvider implements InstallableUnitProvider {

    static final String HINT = "publish-products";

    @Requirement
    private InstallableUnitGenerator installableUnitGenerator;

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        return getProductUnits(installableUnitGenerator, project);
    }

    static Set<IInstallableUnit> getProductUnits(InstallableUnitGenerator installableUnitGenerator,
            MavenProject project) {
        if (PackagingType.TYPE_ECLIPSE_REPOSITORY.equals(project.getPackaging())) {
            //This is already handled there...
            //TODO can we merge the both ways to determine the requirements?
            return Set.of();
        }
        Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-p2-publisher-plugin");
        if (plugin == null || plugin.getExecutions().isEmpty()) {
            return Set.of();
        }
        List<File> productFiles = EclipseRepositoryProject.getProductFiles(project.getBasedir());
        if (productFiles.isEmpty()) {
            return Set.of();
        }
        List<IRequirement> requirements = new ArrayList<>();
        for (File file : productFiles) {
            try {
                Collection<IInstallableUnit> units = installableUnitGenerator
                        .getInstallableUnits(new ProductFile2(file));
                for (IInstallableUnit unit : units) {
                    requirements.addAll(unit.getRequirements());
                }
            } catch (CoreException e) {
            } catch (Exception e) {
            }
        }
        if (requirements.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(InstallableUnitProvider.createIU(requirements, HINT));
    }

}
