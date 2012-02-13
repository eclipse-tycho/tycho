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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.internal.p2.publisher.eclipse.FeatureParser;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.updatesite.CategoryParser;
import org.eclipse.equinox.internal.p2.updatesite.SiteModel;
import org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.p2.impl.publisher.model.ProductFile2;
import org.eclipse.tycho.p2.impl.publisher.repo.FeatureRootfileArtifactRepository;
import org.eclipse.tycho.p2.impl.publisher.repo.TransientArtifactRepository;
import org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdvice;
import org.eclipse.tycho.p2.maven.repository.xmlio.ArtifactsIO;
import org.eclipse.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.eclipse.tycho.p2.metadata.IArtifactFacade;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;

@SuppressWarnings("restriction")
public class P2GeneratorImpl extends AbstractMetadataGenerator implements P2Generator {
    private static final String[] SUPPORTED_TYPES = { ArtifactKey.TYPE_ECLIPSE_PLUGIN,
            ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN, ArtifactKey.TYPE_ECLIPSE_FEATURE,
            ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE, ArtifactKey.TYPE_ECLIPSE_APPLICATION,
            ArtifactKey.TYPE_ECLIPSE_REPOSITORY };

    /**
     * Whether we need full p2 metadata (false) or just required capabilities.
     */
    private boolean dependenciesOnly;

    public P2GeneratorImpl(boolean dependenciesOnly) {
        this.dependenciesOnly = dependenciesOnly;
    }

    // no-args constructor required by DS
    public P2GeneratorImpl() {
        this(false);
    }

    public Map<String, Set<Object>> generateMetadata(List<IArtifactFacade> artifacts,
            Map<String, IArtifactFacade> attachedArtifacts, File targetDir) throws IOException {
        Map<String, Set<Object>> result = new LinkedHashMap<String, Set<Object>>();

        LinkedHashSet<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();
        LinkedHashSet<IArtifactDescriptor> artifactDescriptors = new LinkedHashSet<IArtifactDescriptor>();

        for (IArtifactFacade artifact : artifacts) {
            PublisherInfo publisherInfo = new PublisherInfo();

            DependencyMetadata metadata;

            // meta data handling for root files
            if ("eclipse-feature".equals(artifact.getPackagingType())) {
                publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH
                        | IPublisherInfo.A_NO_MD5);
                FeatureRootfileArtifactRepository artifactsRepository = new FeatureRootfileArtifactRepository(
                        publisherInfo, targetDir);
                publisherInfo.setArtifactRepository(artifactsRepository);

                metadata = super.generateMetadata(artifact, null, publisherInfo, null);

                attachedArtifacts.putAll(artifactsRepository.getPublishedArtifacts());
            } else {
                publisherInfo.setArtifactOptions(IPublisherInfo.A_NO_MD5);
                TransientArtifactRepository artifactsRepository = new TransientArtifactRepository();
                publisherInfo.setArtifactRepository(artifactsRepository);
                metadata = super.generateMetadata(artifact, null, publisherInfo, null);
            }

            units.addAll(metadata.getInstallableUnits());
            artifactDescriptors.addAll(metadata.getArtifactDescriptors());

            // secondary metadata is meant to represent installable units that are provided by this project
            // but do not affect dependencies of the project itself. generateMetadata is called at the end
            // of project build lifecycle, and primary/secondary metadata separation is irrelevant at this point 
            result.put(artifact.getClassidier(), new LinkedHashSet<Object>(metadata.getInstallableUnits()));
        }

        new MetadataIO().writeXML(units, attachedArtifacts.get(RepositoryLayoutHelper.CLASSIFIER_P2_METADATA)
                .getLocation());
        new ArtifactsIO().writeXML(artifactDescriptors,
                attachedArtifacts.get(RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS).getLocation());

        return result;
    }

    public DependencyMetadata generateMetadata(IArtifactFacade artifact, List<Map<String, String>> environments) {
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactOptions(IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH);
        publisherInfo.setArtifactRepository(new TransientArtifactRepository());

        return super.generateMetadata(artifact, environments, publisherInfo, null);
    }

    @Override
    protected List<IPublisherAction> getPublisherActions(IArtifactFacade artifact,
            List<Map<String, String>> environments, OptionalResolutionAction optionalAction) {

        if (!dependenciesOnly && optionalAction != null) {
            throw new IllegalArgumentException();
        }

        List<IPublisherAction> actions = new ArrayList<IPublisherAction>();

        String packaging = artifact.getPackagingType();
        File location = artifact.getLocation();
        if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(packaging) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals(packaging)) {
            if (dependenciesOnly && optionalAction != null) {
                actions.add(new BundleDependenciesAction(location, optionalAction));
            } else {
                actions.add(new TychoBundleAction(location));
            }
        } else if (ArtifactKey.TYPE_ECLIPSE_FEATURE.equals(packaging)) {
            Feature feature = new FeatureParser().parse(location);
            feature.setLocation(location.getAbsolutePath());
            if (dependenciesOnly) {
                actions.add(new FeatureDependenciesAction(feature));
            } else {
                actions.add(new FeaturesAction(new Feature[] { feature }));
            }
        } else if (ArtifactKey.TYPE_ECLIPSE_APPLICATION.equals(packaging)) {
            String product = new File(location, artifact.getArtifactId() + ".product").getAbsolutePath();
            try {
                IProductDescriptor productDescriptor = new ProductFile2(product);
                if (dependenciesOnly) {
                    actions.add(new ProductDependenciesAction(productDescriptor, environments));
                } else {
                    actions.add(new ProductAction(product, productDescriptor, null, null));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE.equals(packaging)) {
            if (dependenciesOnly) {
                actions.add(new SiteDependenciesAction(location, artifact.getArtifactId(), artifact.getVersion()));
            } else {
                actions.add(new SiteXMLAction(location.toURI(), null));
            }
        } else if (ArtifactKey.TYPE_ECLIPSE_REPOSITORY.equals(packaging)) {
            for (File productFile : getProductFiles(location)) {
                String product = productFile.getAbsolutePath();
                IProductDescriptor productDescriptor;
                try {
                    productDescriptor = new ProductFile2(product);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to parse the product file " + product, e);
                }
                if (dependenciesOnly) {
                    actions.add(new ProductDependenciesAction(productDescriptor, environments));
                }
            }
            for (File categoryFile : getCategoryFiles(location)) {
                CategoryParser cp = new CategoryParser(null);
                FileInputStream ins = null;
                try {
                    try {
                        ins = new FileInputStream(categoryFile);
                        SiteModel siteModel = cp.parse(ins);
                        actions.add(new CategoryDependenciesAction(siteModel, artifact.getArtifactId(), artifact
                                .getVersion()));
                    } finally {
                        if (ins != null) {
                            ins.close();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Unable to read category File", e);
                }
            }
        } else if (location.isFile() && location.getName().endsWith(".jar")) {
            actions.add(new TychoBundleAction(location));
        } else {
            throw new IllegalArgumentException("Unknown type of packaging " + packaging);
        }

        return actions;
    }

    public boolean isSupported(String type) {
        return Arrays.asList(SUPPORTED_TYPES).contains(type);
    }

    /**
     * Looks for all files at the base of the project that extension is ".product" Duplicated in the
     * EclipseRepositoryProject
     * 
     * @param projectLocation
     * @return The list of product files to parse for an eclipse-repository project
     */
    private List<File> getProductFiles(File projectLocation) {
        List<File> res = new ArrayList<File>();
        for (File f : projectLocation.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".product")) {
                res.add(f);
            }
        }
        return res;
    }

    private List<File> getCategoryFiles(File projectLocation) {
        List<File> res = new ArrayList<File>();
        File categoryFile = new File(projectLocation, "category.xml");
        if (categoryFile.exists()) {
            res.add(categoryFile);
        }
        return res;
    }

    protected List<IPublisherAdvice> getPublisherAdvice(IArtifactFacade artifact) {
        ArrayList<IPublisherAdvice> advice = new ArrayList<IPublisherAdvice>();
        advice.add(new MavenPropertiesAdvice(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                artifact.getClassidier()));
        advice.add(getExtraEntriesAdvice(artifact));

        IFeatureRootAdvice featureRootAdvice = FeatureRootAdvice.createRootFileAdvice(artifact,
                getBuildPropertiesParser());
        if (featureRootAdvice != null) {
            advice.add(featureRootAdvice);
        }
        return advice;
    }
}
