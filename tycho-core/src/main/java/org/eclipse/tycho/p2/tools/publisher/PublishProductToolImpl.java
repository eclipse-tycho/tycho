/*******************************************************************************
 * Copyright (c) 2010, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - [Issue #80] Incorrect requirement version for configuration/plugins in publish-products (gently sponsored by Compart AG)
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.publisher.DependencySeedUtil.createSeed;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.TouchpointInstruction;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ConfigCUsAction;
import org.eclipse.equinox.p2.publisher.eclipse.IConfigAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.eclipse.equinox.p2.publisher.eclipse.ProductFileAdvice;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.spi.RepositoryReference;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.Interpolator;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.repository.PublishingRepository;
import org.eclipse.tycho.p2.tools.publisher.facade.PublishProductTool;
import org.eclipse.tycho.targetplatform.P2TargetPlatform;
import org.xml.sax.Attributes;

/**
 * Tool for transforming product definition source files into p2 metadata and artifacts. Includes
 * more steps than classic publisher would do, e.g. qualifier expansion.
 */
public class PublishProductToolImpl implements PublishProductTool {

    private final P2TargetPlatform targetPlatform;

    private final PublisherActionRunner publisherRunner;
    private final PublishingRepository publishingRepository;

    private final String buildQualifier;
    private final Interpolator interpolator;
    private final MavenLogger logger;

    public PublishProductToolImpl(PublisherActionRunner publisherRunner, PublishingRepository publishingRepository,
            P2TargetPlatform targetPlatform, String buildQualifier, Interpolator interpolator, MavenLogger logger) {
        this.publisherRunner = publisherRunner;
        this.publishingRepository = publishingRepository;
        this.targetPlatform = targetPlatform;
        this.buildQualifier = buildQualifier;
        this.interpolator = interpolator;
        this.logger = logger;
    }

    @Override
    public List<DependencySeed> publishProduct(File productFile, File launcherBinaries, String flavor)
            throws IllegalArgumentException {

        IProductDescriptor originalProduct = loadProductFile(productFile);
        ExpandedProduct expandedProduct = new ExpandedProduct(originalProduct, buildQualifier, targetPlatform,
                interpolator, logger);

        IPublisherAdvice[] advice = getProductSpecificAdviceFileAdvice(productFile, expandedProduct);

        ProductAction action = new ProductAction(null, expandedProduct, flavor, launcherBinaries) {
            //TODO: Remove this anonymous extension once https://github.com/eclipse-equinox/p2/pull/353 is available
            @Override
            protected IPublisherAction createConfigCUsAction() {
                return new ConfigCUsAction(info, flavor, id, version) {
                    private static final Collection<String> PROPERTIES_TO_SKIP = Set.of("osgi.frameworkClassPath",
                            "osgi.framework", "osgi.bundles", "eof", "eclipse.p2.profile", "eclipse.p2.data.area",
                            "org.eclipse.update.reconcile", "org.eclipse.equinox.simpleconfigurator.configUrl");

                    @Override
                    protected String[] getConfigurationStrings(Collection<IConfigAdvice> configAdvice) {
                        String configurationData = ""; //$NON-NLS-1$
                        String unconfigurationData = ""; //$NON-NLS-1$
                        Set<String> properties = new HashSet<>();
                        for (IConfigAdvice advice : configAdvice) {
                            for (Entry<String, String> aProperty : advice.getProperties().entrySet()) {
                                String key = aProperty.getKey();
                                if (!PROPERTIES_TO_SKIP.contains(key) && !properties.contains(key)) {
                                    properties.add(key);
                                    Map<String, String> parameters = new LinkedHashMap<>();
                                    parameters.put("propName", key); //$NON-NLS-1$
                                    parameters.put("propValue", aProperty.getValue()); //$NON-NLS-1$
                                    configurationData += TouchpointInstruction.encodeAction("setProgramProperty", //$NON-NLS-1$
                                            parameters);
                                    parameters.put("propValue", ""); //$NON-NLS-1$//$NON-NLS-2$
                                    unconfigurationData += TouchpointInstruction.encodeAction("setProgramProperty", //$NON-NLS-1$
                                            parameters);
                                }
                            }
                            if (advice instanceof ProductFileAdvice) {
                                for (IRepositoryReference repo : ((ProductFileAdvice) advice).getUpdateRepositories()) {
                                    Map<String, String> parameters = new LinkedHashMap<>();
                                    parameters.put("type", Integer.toString(repo.getType())); //$NON-NLS-1$
                                    parameters.put("location", repo.getLocation().toString()); //$NON-NLS-1$
                                    if (repo.getNickname() != null) {
                                        parameters.put("name", repo.getNickname()); //$NON-NLS-1$
                                    }
                                    parameters.put("enabled", Boolean.toString( //$NON-NLS-1$
                                            (repo.getOptions() & IRepository.ENABLED) == IRepository.ENABLED));
                                    configurationData += TouchpointInstruction.encodeAction("addRepository", //$NON-NLS-1$
                                            parameters);
                                    parameters.remove("enabled"); //$NON-NLS-1$
                                    unconfigurationData += TouchpointInstruction.encodeAction("removeRepository", //$NON-NLS-1$
                                            parameters);
                                }
                            }
                        }
                        return new String[] { configurationData, unconfigurationData };
                    }
                };
            }
        };
        IMetadataRepository metadataRepository = publishingRepository.getMetadataRepository();
        IArtifactRepository artifactRepository = publishingRepository
                .getArtifactRepositoryForWriting(new ProductBinariesWriteSession(expandedProduct.getId()));
        Collection<IInstallableUnit> allIUs = publisherRunner.executeAction(action, metadataRepository,
                artifactRepository, advice);

        List<DependencySeed> seeds = new ArrayList<>();
        seeds.add(createSeed(ArtifactType.TYPE_ECLIPSE_PRODUCT, selectUnit(allIUs, expandedProduct.getId())));
        addRootFeatures(expandedProduct, seeds);
        return seeds;
    }

    /**
     * In addition to the p2.inf file in the project root (which is automatically picked up by p2,
     * see see {@link ProductAction#createAdviceFileAdvice()}), we allow a "xx.p2.inf" next to a
     * product file "xx.product".
     */
    private static IPublisherAdvice[] getProductSpecificAdviceFileAdvice(File productFile,
            IProductDescriptor expandedProduct) {
        AdviceFileAdvice advice = new AdviceFileAdvice(expandedProduct.getId(),
                Version.parseVersion(expandedProduct.getVersion()), new Path(productFile.getParent()),
                new Path(getProductSpecificP2InfName(productFile.getName())));
        if (advice.containsAdvice()) {
            return new IPublisherAdvice[] { advice };
        } else {
            return new IPublisherAdvice[0];
        }
    }

    private static String getProductSpecificP2InfName(String productFileName) {
        // This must match org.eclipse.tycho.p2.impl.publisher.ProductDependenciesAction.addPublisherAdvice(IPublisherInfo)
        return productFileName.substring(0, productFileName.length() - ".product".length()) + ".p2.inf";
    }

    private static void addRootFeatures(ExpandedProduct product, List<DependencySeed> seeds) {
        final String productId = product.getId();

        // add root features as special dependency seed which are marked as "add-on" for the product
        DependencySeed.Filter filter = (type, id) -> ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(type)
                && productId.equals(id);
        for (IInstallableUnit featureIU : product.getRootFeatures()) {
            ArtifactKey featureArtifact = ArtifactTypeHelper.toTychoArtifact(featureIU);
            seeds.add(new DependencySeed(featureArtifact.getType(), featureArtifact.getId(), featureIU, filter));
        }
    }

    private static IProductDescriptor loadProductFile(File productFile) throws IllegalArgumentException {
        try {
            return new ProductFile(productFile.getAbsolutePath()) {
                //TODO: Remove this anonymous extension once https://github.com/eclipse-equinox/p2/pull/353 is available
                private static final int STATE_REPOSITORIES = 28;
                private static final Field STATE_FILED;
                static {
                    Field state = null;
                    try {
                        state = ProductFile.class.getDeclaredField("state");
                        state.trySetAccessible();
                    } catch (NoSuchFieldException | SecurityException e) {
                    }
                    STATE_FILED = state;
                }

                private int getState() {
                    try {
                        return (Integer) STATE_FILED.get(this);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new IllegalStateException("Failed to get processing state", e);
                    }
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    int state = getState();
                    if (state == STATE_REPOSITORIES && "repository".equals(localName)) {
                        processRepositoryInformation(attributes);
                    } else {
                        super.startElement(uri, localName, qName, attributes);
                    }
                }

                private void processRepositoryInformation(Attributes attributes) {
                    try {
                        List<IRepositoryReference> repositories = getRepositoryEntries();
                        URI uri = URIUtil.fromString(attributes.getValue("location"));
                        String name = attributes.getValue("name");
                        boolean enabled = Boolean.parseBoolean(attributes.getValue("enabled"));
                        int options = enabled ? IRepository.ENABLED : IRepository.NONE;
                        // First add a metadata repository
                        repositories.add(new RepositoryReference(uri, name, IRepository.TYPE_METADATA, options));
                        // Now a colocated artifact repository
                        repositories.add(new RepositoryReference(uri, name, IRepository.TYPE_ARTIFACT, options));
                    } catch (URISyntaxException e) {
                        // ignore malformed URI's. These should have already been caught by the UI
                    }
                }
            };
        } catch (Exception e) {
            throw new BuildFailureException(
                    "Cannot parse product file " + productFile.getAbsolutePath() + ": " + e.getMessage(), e); //$NON-NLS-1$
        }
    }

    private static IInstallableUnit selectUnit(Collection<IInstallableUnit> units, String id) {
        for (IInstallableUnit unit : units) {
            if (id.equals(unit.getId())) {
                return unit;
            }
        }
        throw new AssertionFailedException("Publisher did not produce expected IU");
    }

}
