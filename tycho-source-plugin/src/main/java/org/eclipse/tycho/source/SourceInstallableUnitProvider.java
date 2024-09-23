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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.p2maven.InstallableUnitPublisher;
import org.eclipse.tycho.resolver.InstallableUnitProvider;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * provides a preliminary IU to make generated sources visible to the project dependencies stage
 */
@Singleton
@Named("source")
public class SourceInstallableUnitProvider implements InstallableUnitProvider {

    private final InstallableUnitPublisher publisher;
    private final BundleReader bundleReader;
    private final Map<String, TychoProject> projectTypes;
    private final BuildPropertiesParser buildPropertiesParser;

    @Inject
    public SourceInstallableUnitProvider(InstallableUnitPublisher publisher,
                                         BundleReader bundleReader,
                                         Map<String, TychoProject> projectTypes,
                                         BuildPropertiesParser buildPropertiesParser) {
        this.publisher = publisher;
        this.bundleReader = bundleReader;
        this.projectTypes = projectTypes;
        this.buildPropertiesParser = buildPropertiesParser;
    }

    @Override
    public Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session)
            throws CoreException {
        if (SourceFeatureMojo.isEnabledForProject(project)) {
            try {
                //TODO even though we do not know the exact we might want to still fill in more infos here?
                File sourceFeatureBasedir = SourceFeatureMojo.getSourcesFeatureOutputDir(project);
                Feature sourceFeature = SourceFeatureP2MetadataProvider.createPreliminarySourceFeature(project);
                //TODO instead of writing it to file, we should use P2 Feature Objects directly!
                File file = new File(sourceFeatureBasedir, Feature.FEATURE_XML);
                Feature.write(sourceFeature, file);
                FeaturesAction featuresAction = new FeaturesAction(new File[] { sourceFeatureBasedir });
                return publisher.publishMetadata(List.of(featuresAction));
            } catch (IOException e) {
                throw new CoreException(Status.error("Creating preliminary source feature failed", e));
            }
        }
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
