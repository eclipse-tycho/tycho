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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class SourcesBundleDependencyMetadataGenerator extends AbstractMetadataGenerator
        implements DependencyMetadataGenerator {
    private static final String SUFFIX_QUALIFIER = ".qualifier";

    private static final String SUFFIX_SNAPSHOT = "-SNAPSHOT";

    @Override
    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<TargetEnvironment> environments,
            OptionalResolutionAction optionalAction) {
        return super.generateMetadata(artifact, environments, new PublisherInfo(), optionalAction);
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
    protected List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact) {
        ArrayList<IPublisherAdvice> advice = new ArrayList<>();

        advice.add(new MavenPropertiesAdvice(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                "sources"));

        return advice;
    }

    protected static String toCanonicalVersion(String version) {
        if (version == null) {
            return null;
        }
        if (version.endsWith(SUFFIX_SNAPSHOT)) {
            return version.substring(0, version.length() - SUFFIX_SNAPSHOT.length()) + SUFFIX_QUALIFIER;
        }
        if (version.contains("-") && !version.endsWith(SUFFIX_SNAPSHOT)) {
            return processVersionWithDashes(version);
        }
        return version;
    }

    private static String processVersionWithDashes(String version) {
        List<String> splittedVersion = Arrays.asList(version.split("-"));
        String validOsgiVersion = "";
        if (!splittedVersion.isEmpty() && splittedVersion.size() > 1) {
            String osgiVersion = "";
            if (splittedVersion.get(0).matches("^[0-9]{1}.[0-9]{1}.[0-9]{1}$")) {
                osgiVersion = splittedVersion.get(0) + ".";
            }
            String qualifier = "";
            Iterator<String> iterator = splittedVersion.iterator();
            iterator.next();
            while (iterator.hasNext()) {
                qualifier += iterator.next() + "-";
            }
            validOsgiVersion = osgiVersion + qualifier.substring(0, qualifier.length() - 1);
        }
        return validOsgiVersion;
    }

    public long createId(String sourceBundleSymbolicName, String version) {
        return (long) sourceBundleSymbolicName.hashCode() | (((long) version.hashCode()) << 32);
    }

}
