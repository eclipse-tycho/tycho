/*******************************************************************************
 * Copyright (c) 2008, 2013 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Marco Lehmann-Mörz - issue #2877 - tycho-versions-plugin:bump-versions does not honor SNAPSHOT suffix
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.tycho.p2maven.tmp.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.tycho.BuildPropertiesParser;
import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.publisher.TychoMavenPropertiesAdvice;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.AbstractMetadataGenerator;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.publisher.DownloadStatsAdvice;
import org.osgi.framework.BundleException;

@Component(role = DependencyMetadataGenerator.class, hint = DependencyMetadataGenerator.SOURCE_BUNDLE)
public class SourcesBundleDependencyMetadataGenerator extends AbstractMetadataGenerator
        implements DependencyMetadataGenerator {

    @Requirement
    private MavenContext mavenContext;

    @Requirement
    private BuildPropertiesParser buildPropertiesParser;

    @Override
    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction, PublisherOptions options) {
        return super.generateMetadata(artifact, environments, new PublisherInfo(), optionalAction, options);
    }

    @Override
    protected List<IPublisherAction> getPublisherActions(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction) {
        ArrayList<IPublisherAction> actions = new ArrayList<>();

        String id = artifact.getArtifactId();
        String version = toCanonicalVersion(artifact.getVersion());
        try {
            // generated source bundle is not available at this point in filesystem yet, need to create
            // in-memory BundleDescription instead
            Dictionary<String, String> manifest = new Hashtable<>();
            manifest.put("Manifest-Version", "1.0");
            manifest.put("Bundle-ManifestVersion", "2");
            String sourceBundleSymbolicName = id + ".source";
            manifest.put("Bundle-SymbolicName", sourceBundleSymbolicName);
            manifest.put("Bundle-Version", version);
            manifest.put("Eclipse-SourceBundle", id + ";version=" + version + ";roots:=\".\"");
            StateObjectFactory factory = StateObjectFactory.defaultFactory;
            BundleDescription bundleDescription = factory.createBundleDescription(factory.createState(false), manifest,
                    artifact.getLocation().getAbsolutePath(), createId(sourceBundleSymbolicName, version));
            bundleDescription.setUserObject(manifest);
            actions.add(new BundlesAction(new BundleDescription[] { bundleDescription }) {
                @Override
                protected void createAdviceFileAdvice(BundleDescription bundleDescription,
                        IPublisherInfo publisherInfo) {
                    // 367255 p2.inf is not applicable to sources bundles
                }
            });
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }

        return actions;
    }

    @Override
    protected List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact, PublisherOptions options) {
        ArrayList<IPublisherAdvice> advice = new ArrayList<>();

        advice.add(new TychoMavenPropertiesAdvice(artifact, "sources", mavenContext));

        if (options.isGenerateDownloadStats()) {
            advice.add(new DownloadStatsAdvice());
        }

        return advice;
    }

    private static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }
        if (version.endsWith(TychoConstants.SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - TychoConstants.SUFFIX_SNAPSHOT.length())
                    + TychoConstants.SUFFIX_QUALIFIER;
        }
        return version;
    }

    public long createId(String sourceBundleSymbolicName, String version) {
        return sourceBundleSymbolicName.hashCode() | (((long) version.hashCode()) << 32);
    }

    @Override
    protected BuildPropertiesParser getBuildPropertiesParser() {
        return buildPropertiesParser;
    }

    public void setMavenContext(MavenContext mockMavenContext) {
        mavenContext = mockMavenContext;
    }

}
