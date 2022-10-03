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
package org.eclipse.tycho.p2.publisher;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.IMatchExpression;
import org.eclipse.equinox.p2.publisher.AdviceFileAdvice;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

@SuppressWarnings("restriction")
public class FeatureDependenciesAction extends AbstractDependenciesAction {
    /**
     * Comma separated list of IInstallableUnit ids that are included (as opposed to required by)
     * the feature.
     */
    public static final String INCLUDED_IUS = "org.eclipse.tycho.p2.includedIUs";

    private final Feature feature;

    public FeatureDependenciesAction(Feature feature) {
        this.feature = feature;
    }

    private String getInstallableUnitId(FeatureEntry entry) {
        if (entry.isPlugin()) {
            return entry.getId();
        } else {
            return entry.getId() + FEATURE_GROUP_IU_SUFFIX;
        }
    }

    /**
     * Copy&Paste from 3.7
     * org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction.getVersionRange(FeatureEntry)
     */
    private VersionRange getVersionRange(FeatureEntry entry) {
        String versionSpec = entry.getVersion();
        if (versionSpec == null)
            return VersionRange.emptyRange;
        String match = entry.getMatch();
        if ("versionRange".equals(match)) //$NON-NLS-1$
            return new VersionRange(versionSpec);
        Version version = Version.parseVersion(versionSpec);
        if (version.equals(Version.emptyVersion))
            return VersionRange.emptyRange;
        if (!entry.isRequires())
            return new VersionRange(version, true, version, true);
        if (match == null)
            // TODO should really be returning VersionRange.emptyRange here...
            return null;
        if (match.equals("perfect")) //$NON-NLS-1$
            return new VersionRange(version, true, version, true);

        org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(version);
        if (match.equals("equivalent")) { //$NON-NLS-1$
            Version upper = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor() + 1, 0);
            return new VersionRange(version, true, upper, false);
        }
        if (match.equals("compatible")) { //$NON-NLS-1$
            Version upper = Version.createOSGi(osgiVersion.getMajor() + 1, 0, 0);
            return new VersionRange(version, true, upper, false);
        }
        if (match.equals("greaterOrEqual")) //$NON-NLS-1$
            return new VersionRange(version, true, new VersionRange(null).getMaximum(), true);
        return null;
    }

    @Override
    protected Set<IRequirement> getRequiredCapabilities() {
        Set<IRequirement> required = new LinkedHashSet<>();

        if (feature.getLicenseFeature() != null) {
            String id = feature.getLicenseFeature() + FEATURE_GROUP_IU_SUFFIX;
            VersionRange range = getVersionRange(createVersion(feature.getLicenseFeatureVersion()));
            required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range, null,
                    false/* optional */, false /* multiple */));
        }

        for (FeatureEntry entry : feature.getEntries()) {
            if (entry.isPatch()) {
                // this entry declares the feature being patch. it does not introduce any additional content in the
                // target platform
                continue;
            }
            VersionRange range;
            if (entry.isRequires()) {
                range = getVersionRange(entry);
            } else {
                range = getVersionRange(createVersion(entry.getVersion()));
            }
            String id = getInstallableUnitId(entry);
            // TODO 391283 without enhancement 391283, additional filters will always evaluate to false -> ignore for now
            boolean optional = entry.isOptional();
            required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range,
                    createFilter(entry), optional, false));
        }
        return required;
    }

    @Override
    protected Version getVersion() {
        return Version.create(feature.getVersion());
    }

    @Override
    protected String getId() {
        return feature.getId() + FEATURE_GROUP_IU_SUFFIX;
    }

    @Override
    protected void addProvidedCapabilities(Set<IProvidedCapability> provided) {
        provided.add(MetadataFactory.createProvidedCapability(PublisherHelper.CAPABILITY_NS_UPDATE_FEATURE,
                feature.getId(), getVersion()));
    }

    @Override
    protected void addProperties(InstallableUnitDescription iud) {
        iud.setProperty(QueryUtil.PROP_TYPE_GROUP, "true");

        StringJoiner includedIUs = new StringJoiner(",");
        for (FeatureEntry entry : feature.getEntries()) {
            String id = getInstallableUnitId(entry);
            includedIUs.add(id);
        }
        iud.setProperty(INCLUDED_IUS, includedIUs.toString());
    }

    @Override
    protected void addPublisherAdvice(IPublisherInfo publisherInfo) {
        // see org.eclipse.equinox.p2.publisher.eclipse.FeaturesAction.createAdviceFileAdvice(Feature, IPublisherInfo)
        IPath location = new Path(feature.getLocation());
        Version version = Version.parseVersion(feature.getVersion());
        String groupId = getId();
        AdviceFileAdvice advice = new AdviceFileAdvice(groupId, version, location, new Path("p2.inf"));
        if (advice.containsAdvice()) {
            publisherInfo.addAdvice(advice);
        }
    }

    private IMatchExpression<IInstallableUnit> createFilter(FeatureEntry entry) {
        final StringBuilder result = new StringBuilder();
        if (entry.getFilter() != null) {
            result.append(entry.getFilter());
        }
        expandFilter(entry.getOS(), "osgi.os", result); //$NON-NLS-1$
        expandFilter(entry.getWS(), "osgi.ws", result); //$NON-NLS-1$
        expandFilter(entry.getArch(), "osgi.arch", result);//$NON-NLS-1$
        expandFilter(entry.getNL(), "osgi.nl", result); //$NON-NLS-1$
        if (result.length() != 0) {
            if (getFilterCount(entry) > 1) {
                result.insert(0, "(&").append(')'); //$NON-NLS-1$
            }
            return InstallableUnit.parseFilter(result.toString());
        }
        return null;
    }

    private int getFilterCount(FeatureEntry entry) {
        int filterCount = 0;
        if (entry.getOS() != null && !entry.getOS().isEmpty()) {
            filterCount++;
        }
        if (entry.getWS() != null && !entry.getWS().isEmpty()) {
            filterCount++;
        }
        if (entry.getArch() != null && !entry.getArch().isEmpty()) {
            filterCount++;
        }
        if (entry.getNL() != null && !entry.getNL().isEmpty()) {
            filterCount++;
        }
        if (entry.getFilter() != null && !entry.getFilter().isEmpty()) {
            filterCount++;
        }
        return filterCount;
    }

    private void expandFilter(String filter, String osgiFilterValue, StringBuilder result) {
        if (filter != null && !filter.isEmpty()) {
            final StringTokenizer token = new StringTokenizer(filter, ","); //$NON-NLS-1$
            if (token.countTokens() == 1) {
                result.append('(' + osgiFilterValue + '=' + filter + ')');
            } else {
                result.append("(|"); //$NON-NLS-1$
                while (token.hasMoreElements()) {
                    result.append('(' + osgiFilterValue + '=' + token.nextToken() + ')');
                }
                result.append(')');
            }
        }
    }

    public static Set<String> getIncludedUIs(IInstallableUnit iu) {
        Set<String> includedIUs = new LinkedHashSet<>();

        String prop = iu.getProperty(INCLUDED_IUS);
        if (prop != null) {
            StringTokenizer st = new StringTokenizer(prop, ",");
            while (st.hasMoreTokens()) {
                includedIUs.add(st.nextToken());
            }
        }
        return includedIUs;
    }
}
