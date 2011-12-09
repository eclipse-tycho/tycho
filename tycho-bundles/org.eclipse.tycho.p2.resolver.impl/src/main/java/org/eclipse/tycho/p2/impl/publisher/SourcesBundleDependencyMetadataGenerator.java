/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.IReactorArtifactFacade;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class SourcesBundleDependencyMetadataGenerator extends AbstractMetadataGenerator implements
        DependencyMetadataGenerator {
    private static final String SUFFIX_QUALIFIER = ".qualifier";

    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    public Set<Object/* IInstallableUnit */> generateMetadata(IArtifactFacade artifact,
            List<Map<String, String>> environments, OptionalResolutionAction optionalAction) {
        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        PublisherInfo publisherInfo = new PublisherInfo();
        super.generateMetadata(artifact, environments, units, artifactDescriptors, publisherInfo, optionalAction);

        return new LinkedHashSet<Object>(units);
    }

    @Override
    protected List<IPublisherAction> getPublisherActions(IArtifactFacade artifact,
            List<Map<String, String>> environments, OptionalResolutionAction optionalAction) {
        ArrayList<IPublisherAction> actions = new ArrayList<IPublisherAction>();

        String id = artifact.getArtifactId();
        String version = toCanonicalVersion(artifact.getVersion());
        try {
            // generated source bundle is not available at this point in filesystem yet, need to create
            // in-memory BundleDescription instead
            Dictionary<String, String> manifest = new Hashtable<String, String>();
            manifest.put("Manifest-Version", "1.0");
            manifest.put("Bundle-ManifestVersion", "2");
            String sourceBundleSymbolicName = id + IReactorArtifactFacade.SOURCE_BUNDLE_SUFFIX;
            manifest.put("Bundle-SymbolicName", sourceBundleSymbolicName);
            manifest.put("Bundle-Version", version);
            manifest.put("Eclipse-SourceBundle", id + ";version=" + version + ";roots:=\".\"");
            StateObjectFactory factory = StateObjectFactory.defaultFactory;
            BundleDescription bundleDescription = factory.createBundleDescription(factory.createState(false), manifest,
                    artifact.getLocation().getAbsolutePath(), createId(sourceBundleSymbolicName, version));
            bundleDescription.setUserObject(manifest);
            actions.add(new TychoBundleAction(bundleDescription));
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }

        return actions;
    }

    @Override
    protected List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact) {
        ArrayList<IPublisherAdvice> advice = new ArrayList<IPublisherAdvice>();

        advice.add(new MavenPropertiesAdvice(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                "sources"));

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
        return (long) sourceBundleSymbolicName.hashCode() | (((long) version.hashCode()) << 32);
    }

}
