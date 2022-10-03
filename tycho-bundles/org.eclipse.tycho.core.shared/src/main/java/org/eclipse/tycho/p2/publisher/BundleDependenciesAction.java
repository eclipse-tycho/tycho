/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.publisher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.AdviceFileParser;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.OptionalResolutionAction;

@SuppressWarnings("restriction")
public class BundleDependenciesAction extends BundlesAction {

    /**
     * If true, treat optional Import-Package and Require-Bundle as required. If false, optional
     * Import-Package and Require-Bundle are ignored.
     */
    private final OptionalResolutionAction optionalAction;

    public BundleDependenciesAction(File location, OptionalResolutionAction optionalAction) {
        super(new File[] { location });
        this.optionalAction = optionalAction;
    }

    @Override
    protected void addImportPackageRequirement(List<IRequirement> reqsDeps, ImportPackageSpecification importSpec,
            ManifestElement[] rawImportPackageHeader) {
        if (optionalAction == OptionalResolutionAction.OPTIONAL) {
            super.addImportPackageRequirement(reqsDeps, importSpec, rawImportPackageHeader);
            return;
        }
        VersionRange versionRange = PublisherHelper.fromOSGiVersionRange(importSpec.getVersionRange());
        final boolean required = !isOptional(importSpec) || optionalAction == OptionalResolutionAction.REQUIRE;
        if (required) {
            //TODO this needs to be refined to take into account all the attribute handled by imports
            reqsDeps.add(MetadataFactory.createRequirement(PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE,
                    importSpec.getName(), versionRange, null, 1, 1, true /* greedy */));
        }
    }

    @Override
    protected void addRequireBundleRequirement(List<IRequirement> reqsDeps, BundleSpecification requiredBundle,
            ManifestElement[] rawRequireBundleHeader) {
        if (optionalAction == OptionalResolutionAction.OPTIONAL) {
            super.addRequireBundleRequirement(reqsDeps, requiredBundle, rawRequireBundleHeader);
            return;
        }
        VersionRange versionRange = PublisherHelper.fromOSGiVersionRange(requiredBundle.getVersionRange());
        final boolean required = !requiredBundle.isOptional() || optionalAction == OptionalResolutionAction.REQUIRE;
        if (required) {
            reqsDeps.add(MetadataFactory.createRequirement(CAPABILITY_NS_OSGI_BUNDLE, requiredBundle.getName(),
                    versionRange, null, 1, 1, true /* greedy */));
        }
    }

    @Override
    protected void createAdviceFileAdvice(BundleDescription bundleDescription, IPublisherInfo publisherInfo) {
        String location = bundleDescription.getLocation();
        if (location == null)
            return;

        File adviceFile = new File(location, AdviceFileAdvice.BUNDLE_ADVICE_FILE.toString());

        if (!adviceFile.canRead()) {
            return;
        }

        Map<String, String> advice = new LinkedHashMap<>();

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(adviceFile));
            try {
                Properties props = new Properties();
                props.load(is);
                for (Map.Entry<Object, Object> p : props.entrySet()) {
                    advice.put((String) p.getKey(), (String) p.getValue());
                }
            } finally {
                try {
                    is.close();
                } catch (IOException secondary) {
                    // secondary exception
                }
            }
        } catch (IOException e) {
            // TODO log
            return;
        }

        final String symbolicName = bundleDescription.getSymbolicName();
        final Version bundleVersion = PublisherHelper.fromOSGiVersion(bundleDescription.getVersion());
        AdviceFileParser parser = new AdviceFileParser(symbolicName, bundleVersion, advice) {
            @Override
            protected IRequirement createRequirement(String namespace, String name, VersionRange range, String filter,
                    boolean optional, boolean multiple, boolean greedy) {
                if (optionalAction == OptionalResolutionAction.OPTIONAL) {
                    return super.createRequirement(namespace, name, range, filter, optional, multiple, greedy);
                }
                return BundleDependenciesAction.this.createRequirement(namespace, name, range, filter, optional,
                        multiple, greedy);
            }
        };
        try {
            parser.parse();
        } catch (Exception e) {
            // TODO log
            return;
        }

        final IProvidedCapability[] provided = parser.getProvidedCapabilities();
        final IRequirement[] required = parser.getRequiredCapabilities();

        if (provided == null && required == null) {
            return;
        }

        publisherInfo.addAdvice(new ICapabilityAdvice() {
            @Override
            public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
                return symbolicName.equals(id) && bundleVersion.equals(version);
            }

            @Override
            public IRequirement[] getRequiredCapabilities(InstallableUnitDescription iu) {
                return required;
            }

            @Override
            public IProvidedCapability[] getProvidedCapabilities(InstallableUnitDescription iu) {
                return provided;
            }

            @Override
            public IRequirement[] getMetaRequiredCapabilities(InstallableUnitDescription iu) {
                return null;
            }
        });
    }

    IRequirement createRequirement(String namespace, String name, VersionRange range, String filter, boolean optional,
            boolean multiple, boolean greedy) {
        if (optional && optionalAction == OptionalResolutionAction.IGNORE) {
            return null;
        }
        return MetadataFactory.createRequirement(namespace, name, range, filter, false/* optional */, multiple,
                true/* greedy */);
    }

}
