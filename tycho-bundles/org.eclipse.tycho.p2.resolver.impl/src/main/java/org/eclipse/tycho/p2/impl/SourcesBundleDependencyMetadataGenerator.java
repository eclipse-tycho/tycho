/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2.publisher.AbstractMetadataGenerator;
import org.eclipse.tycho.p2.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.publisher.DownloadStatsAdvice;
import org.eclipse.tycho.p2.publisher.MavenPropertiesAdvice;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class SourcesBundleDependencyMetadataGenerator extends AbstractMetadataGenerator
        implements DependencyMetadataGenerator {
    private static final String SUFFIX_QUALIFIER = ".qualifier";

    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    private MavenContext mavenContext;

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

        advice.add(new MavenPropertiesAdvice(artifact, "sources", mavenContext));

        if (options.generateDownloadStatsProperty) {
            advice.add(new DownloadStatsAdvice());
        }

        return advice;
    }

    private static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }
        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length()) + SUFFIX_QUALIFIER;
        }
        return version;
    }

    public long createId(String sourceBundleSymbolicName, String version) {
        return sourceBundleSymbolicName.hashCode() | (((long) version.hashCode()) << 32);
    }

    public void setMavenContext(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
    }

}
