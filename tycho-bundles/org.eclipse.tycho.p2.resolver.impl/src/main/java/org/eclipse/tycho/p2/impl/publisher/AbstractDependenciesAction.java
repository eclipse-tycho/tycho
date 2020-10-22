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
package org.eclipse.tycho.p2.impl.publisher;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.InstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.publisher.AbstractPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.PublisherResult;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;

@SuppressWarnings("restriction")
public abstract class AbstractDependenciesAction extends AbstractPublisherAction {

    protected static final Version OSGi_versionMin = Version.createOSGi(0, 0, 0);

    /**
     * Conventional qualifier used to denote "ANY QUALIFIER" in feature.xml and .product files. See
     * TYCHO-383.
     */
    protected static final String ANY_QUALIFIER = P2Resolver.ANY_QUALIFIER;

    /**
     * copy&paste from e3.5.1 org.eclipse.osgi.internal.resolver.StateImpl
     */
    protected static final String OSGI_OS = "osgi.os";

    /**
     * copy&paste from e3.5.1 org.eclipse.osgi.internal.resolver.StateImpl
     */
    protected static final String OSGI_WS = "osgi.ws";

    /**
     * copy&paste from e3.5.1 org.eclipse.osgi.internal.resolver.StateImpl
     */
    protected static final String OSGI_ARCH = "osgi.arch";

    /**
     * copy&paste from e3.5.1 org.eclipse.osgi.internal.resolver.StateImpl
     */
    protected static final String OSGI_NL = "osgi.nl";

    protected static final String FEATURE_GROUP_IU_SUFFIX = ".feature.group";

    protected void addRequiredCapability(Set<IRequirement> required, String id, Version version, String filter,
            boolean optional) {
        VersionRange range = getVersionRange(version);

        required.add(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, id, range,
                InstallableUnit.parseFilter(filter), optional, false));
    }

    @Override
    public IStatus perform(IPublisherInfo publisherInfo, IPublisherResult results, IProgressMonitor monitor) {
        InstallableUnitDescription iud = new MetadataFactory.InstallableUnitDescription();
        iud.setId(getId());
        iud.setVersion(getVersion());

        Set<IProvidedCapability> provided = new LinkedHashSet<>();
        addProvidedCapabilities(provided);
        provided.add(MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, iud.getId(),
                iud.getVersion()));
        iud.addProvidedCapabilities(provided);

        iud.addRequirements(getRequiredCapabilities());

        addProperties(iud);
        addPublisherAdvice(publisherInfo);

        processCapabilityAdvice(iud, publisherInfo);
        processInstallableUnitPropertiesAdvice(iud, publisherInfo);

        IInstallableUnit iu = MetadataFactory.createInstallableUnit(iud);
        results.addIU(iu, PublisherResult.ROOT);

        InstallableUnitDescription[] others = processAdditionalInstallableUnitsAdvice(iu, publisherInfo);
        if (others != null) {
            for (InstallableUnitDescription other : others) {
                // using PublisherResult.NON_ROOT results in these IUs appear after the primary
                // see org.eclipse.equinox.p2.publisher.PublisherResult.getIUs(String, String)
                // see org.eclipse.tycho.p2.metadata.IReactorArtifactFacade.getDependencyMetadata()
                results.addIU(MetadataFactory.createInstallableUnit(other), PublisherResult.NON_ROOT);
            }
        }

        return Status.OK_STATUS;
    }

    protected void addPublisherAdvice(IPublisherInfo publisherInfo) {
        // do nothing by default
    }

    protected void addProperties(InstallableUnitDescription iud) {
        // do nothing by default
    }

    protected abstract Set<IRequirement> getRequiredCapabilities();

    protected void addProvidedCapabilities(Set<IProvidedCapability> provided) {

    }

    protected abstract Version getVersion();

    protected abstract String getId();

    protected VersionRange getVersionRange(String version) {
        if (version == null) {
            return VersionRange.emptyRange;
        }

        return getVersionRange(Version.create(version));
    }

    /**
     * @see org.eclipse.tycho.artifacts.DependencyArtifacts.getArtifact(String, String, String)
     */
    protected VersionRange getVersionRange(Version version) {
        if (version == null || OSGi_versionMin.equals(version)) {
            return VersionRange.emptyRange;
        }

        org.osgi.framework.Version osgiVersion = PublisherHelper.toOSGiVersion(version);

        String qualifier = osgiVersion.getQualifier();

        if (qualifier == null || "".equals(qualifier) || ANY_QUALIFIER.equals(qualifier)) {
            Version from = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro());
            Version to = Version.createOSGi(osgiVersion.getMajor(), osgiVersion.getMinor(), osgiVersion.getMicro() + 1);
            return new VersionRange(from, true, to, false);
        }

        return new VersionRange(version, true, version, true);
    }

    protected Version createVersion(String version) {
        return Version.create(version);
    }

}
