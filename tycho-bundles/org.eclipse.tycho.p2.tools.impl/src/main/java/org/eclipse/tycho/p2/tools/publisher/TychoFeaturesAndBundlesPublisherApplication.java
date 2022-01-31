/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
public class TychoFeaturesAndBundlesPublisherApplication extends AbstractPublisherApplication {

    private static final String MAVEN_PREFIX = "maven-";
    private BundleDescription[] bundles;
    private File[] advices;
    private String[] signatures;
    private URI categoryDefinition;
    private String[] rules;
    private String publicKeys;

    @Override
    public Object run(PublisherInfo publisherInfo) throws Exception {
        Object run = super.run(publisherInfo);
        if (run == IApplication.EXIT_OK) {
            if (rules != null && artifactLocation != null) {
                String[][] newRules = new String[rules.length][];
                for (int i = 0; i < rules.length; i++) {
                    newRules[i] = rules[i].split(";", 2);
                }

                SimpleArtifactRepositoryFactory repoFactory = new SimpleArtifactRepositoryFactory();
                SimpleArtifactRepository repo = (SimpleArtifactRepository) repoFactory.load(artifactLocation,
                        IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
                repo.setRules(newRules);
                if (publicKeys != null) {
                    repo.setProperty("pgp.publicKeys", publicKeys);
                }
                repo.save();
            }
        }
        return run;
    }

    @Override
    protected void processParameter(String arg, String parameter, PublisherInfo publisherInfo)
            throws URISyntaxException {
        super.processParameter(arg, parameter, publisherInfo);

        if (arg.equalsIgnoreCase("-bundles")) {
            bundles = Arrays.stream(getArrayFromFile(parameter)).map(File::new).map(t -> {
                try {
                    return BundlesAction.createBundleDescription(t);
                } catch (IOException | BundleException e) {
                    //ignoring files that are "not bundles" they will be skipped on the later steps
                    System.out.println("Ignore " + t.getName() + " as it is not a bundle!");
                    return null;
                }
            }).toArray(BundleDescription[]::new);
        }
        if (arg.equalsIgnoreCase("-advices")) {
            advices = Arrays.stream(getArrayFromFile(parameter)).map(str -> str.isBlank() ? null : new File(str))
                    .toArray(File[]::new);
        }
        if (arg.equalsIgnoreCase("-signatures")) {
            signatures = Arrays.stream(getArrayFromFile(parameter)).map(str -> str.isBlank() ? null : new File(str))
                    .map(file -> {
                        if (file == null) {
                            return null;
                        }
                        try {
                            return Files.readString(file.toPath(), StandardCharsets.US_ASCII);
                        } catch (IOException e) {
                            // skip unreadable files...
                            return null;
                        }
                    }).toArray(String[]::new);
        }
        if (arg.equalsIgnoreCase("-categoryDefinition")) {
            categoryDefinition = URIUtil.fromString(parameter);
        }
        if (arg.equalsIgnoreCase("-rules")) {
            rules = AbstractPublisherAction.getArrayFromString(parameter, ",");
        }
        if (arg.equalsIgnoreCase("-publicKeys")) {
            try {
                publicKeys = Files.readString(Paths.get(parameter), StandardCharsets.US_ASCII);
            } catch (IOException e) {
                throw new URISyntaxException(parameter, "can't read public key file: " + e);
            }
        }
    }

    private String[] getArrayFromFile(String parameter) {
        File file = new File(parameter);
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).toArray(String[]::new);
        } catch (IOException e) {
            throw new RuntimeException("reading file parameter failed", e);
        }
    }

    @Override
    protected IPublisherAction[] createActions() {
        List<IPublisherAction> result = new ArrayList<>();
        if (advices != null) {
            List<AdviceFileAdvice> advicesList = new ArrayList<>();
            for (int i = 0; i < advices.length; i++) {
                File adviceFile = advices[i];
                BundleDescription bundleDescription;
                if (i >= bundles.length || (bundleDescription = bundles[i]) == null) {
                    continue;
                }
                String symbolicName = bundleDescription.getSymbolicName();
                if (symbolicName == null) {
                    //not a bundle... no advice...
                    continue;
                }
                advicesList.add(new AdviceFileAdvice(symbolicName,
                        PublisherHelper.fromOSGiVersion(bundleDescription.getVersion()),
                        new Path(adviceFile.getParentFile().getAbsolutePath()), new Path(adviceFile.getName())) {

                    @Override
                    public Map<String, String> getArtifactProperties(IInstallableUnit iu,
                            IArtifactDescriptor descriptor) {

                        // workaround Bug 539672
                        Map<String, String> properties = super.getInstallableUnitProperties(null);
                        if (properties != null) {
                            if (descriptor instanceof ArtifactDescriptor) {
                                ArtifactDescriptor artifactDescriptor = (ArtifactDescriptor) descriptor;
                                for (Map.Entry<String, String> entry : properties.entrySet()) {
                                    String key = entry.getKey();
                                    String value = entry.getValue();
                                    if (key.startsWith(MAVEN_PREFIX)) {
                                        String key2 = "maven." + key.substring(MAVEN_PREFIX.length());
                                        artifactDescriptor.setProperty(key2, value);
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    @Override
                    public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
                        Map<String, String> properties = super.getInstallableUnitProperties(iu);
                        if (properties == null) {
                            return null;
                        }
                        Map<String, String> installableUnitProperties = new LinkedHashMap<>(properties);
                        Set<String> keySet = installableUnitProperties.keySet();
                        for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {
                            String string = iterator.next();
                            if (string.startsWith(MAVEN_PREFIX)) {
                                iterator.remove();
                            }
                        }
                        return installableUnitProperties.isEmpty() ? null : installableUnitProperties;
                    }

                });
                result.add(new AdviceFilePublisherAction(advicesList));
            }
        }
        if (signatures != null) {
            List<PGPSignatureAdvice> signaturesList = new ArrayList<>();
            for (int i = 0; i < signatures.length; i++) {
                String signature = signatures[i];
                if (signature == null) {
                    //no signature...
                    continue;
                }
                BundleDescription bundleDescription;
                if (i >= bundles.length || (bundleDescription = bundles[i]) == null) {
                    continue;
                }
                String symbolicName = bundleDescription.getSymbolicName();
                if (symbolicName == null) {
                    //not a bundle... no signature...
                    continue;
                }
                signaturesList.add(new PGPSignatureAdvice(symbolicName,
                        PublisherHelper.fromOSGiVersion(bundleDescription.getVersion()), signature, publicKeys));
            }
            result.add(new SignaturePublisherAction(signaturesList));
        }
        if (bundles != null) {
            result.add(new BundlesAction(bundles));
        }
        if (categoryDefinition != null) {
            result.add(new CategoryXMLAction(categoryDefinition, "category"));
        }
        return result.toArray(IPublisherAction[]::new);
    }

    private static final class SignaturePublisherAction extends AbstractPublisherAction {

        private List<PGPSignatureAdvice> signaturesList;

        public SignaturePublisherAction(List<PGPSignatureAdvice> signaturesList) {
            this.signaturesList = signaturesList;
        }

        @Override
        public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
            for (PGPSignatureAdvice signature : signaturesList) {
                publisherInfo.addAdvice(signature);
            }
            return Status.OK_STATUS;
        }

    }

    private static final class AdviceFilePublisherAction extends AbstractPublisherAction {

        private List<AdviceFileAdvice> advicesList;

        public AdviceFilePublisherAction(List<AdviceFileAdvice> advicesList) {
            this.advicesList = advicesList;
        }

        @Override
        public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
            for (AdviceFileAdvice advice : advicesList) {
                publisherInfo.addAdvice(advice);
            }
            return Status.OK_STATUS;
        }

    }

}
