/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.tycho.source;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2maven.InstallableUnitPublisher;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.tycho.resolver.InstallableUnitProvider;
import org.osgi.framework.Constants;

/**
 * provides a preliminary IU to make generated sources visible to the project dependencies stage
 */
@Component(role = InstallableUnitProvider.class, hint = "source")
public class SourceInstallableUnitProvider implements InstallableUnitProvider {

    @Requirement
    private InstallableUnitPublisher publisher;

    @Requirement
    private BundleReader bundleReader;

    @Requirement(role = TychoProject.class)
    private Map<String, TychoProject> projectTypes;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        if (OsgiSourceMojo.isRelevant(project, buildPropertiesParser)) {
            TychoProject projectType = projectTypes.get(project.getPackaging());
            ArtifactKey artifactKey = projectType.getArtifactKey(DefaultReactorProject.adapt(project));
            String symbolicName = artifactKey.getId();
            String version = artifactKey.getVersion();
            Dictionary<String, String> manifest = new Hashtable<>();
            manifest.put("Manifest-Version", "1.0");
            manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
            manifest.put(Constants.BUNDLE_SYMBOLICNAME, String.format("%s.source", symbolicName));
            manifest.put(Constants.BUNDLE_VERSION, version);
            manifest.put(OsgiSourceMojo.MANIFEST_HEADER_ECLIPSE_SOURCE_BUNDLE,
                    String.format("%s;version=%s;roots:=\".\"", symbolicName, version));
            BundleDescription bundleDescription = BundlesAction.createBundleDescription(manifest, project.getBasedir());
            if (bundleDescription != null) {
                return publisher
                        .publishMetadata(List.of(new BundlesAction(new BundleDescription[] { bundleDescription }) {
                            @Override
                            protected void createAdviceFileAdvice(BundleDescription bundleDescription,
                                    IPublisherInfo publisherInfo) {
                                //no advice please...
                            }
                        }));
            }
        }
        return Collections.emptyList();
    }

}
