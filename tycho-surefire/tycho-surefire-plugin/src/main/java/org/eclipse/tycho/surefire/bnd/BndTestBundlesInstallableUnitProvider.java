/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich. and others.
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
package org.eclipse.tycho.surefire.bnd;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.resolver.InstallableUnitProvider;
import org.eclipse.tycho.surefire.BndTestMojo;

/**
 * This provides P2 visible meta-data for bundles that are not expressed in the manifest (e.g.
 * build.properties derived)
 *
 */
@Named(BndTestMojo.NAME)
@Singleton
public class BndTestBundlesInstallableUnitProvider implements InstallableUnitProvider {
    @Inject
    private Map<String, TychoProject> projectTypes;

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        if (projectTypes.get(project.getPackaging()) instanceof BundleProject) {
            Plugin plugin = project.getPlugin("org.eclipse.tycho:tycho-surefire-plugin");
            if (plugin != null) {

                List<String> testbundles = plugin.getExecutions().stream()
                        .filter(pe -> pe.getGoals().contains(BndTestMojo.NAME)).map(pe -> pe.getConfiguration())
                        .filter(Xpp3Dom.class::isInstance).map(Xpp3Dom.class::cast).flatMap(dom -> {
                            Xpp3Dom child = dom.getChild("bundles");
                            if (child == null) {
                                return Stream.empty();
                            }
                            if (child.getChildCount() == 0) {
                                String value = child.getValue();
                                if (value.isBlank()) {
                                    return Stream.empty();
                                }
                                return Arrays.stream(value.split(",")).map(String::trim);
                            } else {
                                return Arrays.stream(child.getChildren()).map(Xpp3Dom::getValue);
                            }
                        }).toList();
                List<IRequirement> additionalBundleRequirements = testbundles.stream()
                        .map(bundleName -> MetadataFactory.createRequirement(BundlesAction.CAPABILITY_NS_OSGI_BUNDLE,
                                bundleName, VersionRange.emptyRange, null, true, true))
                        .toList();
                return createIU(additionalBundleRequirements);
            }
        }
        return Collections.emptyList();
    }

    private Collection<IInstallableUnit> createIU(List<IRequirement> additionalBundleRequirements) {
        if (additionalBundleRequirements.isEmpty()) {
            return Collections.emptyList();
        }
        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        result.setId("bnd-test-bundles-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
        result.addRequirements(additionalBundleRequirements);
        return List.of(MetadataFactory.createInstallableUnit(result));
    }

}
