/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - add to string for easier debugging
 *******************************************************************************/
package org.eclipse.tycho.core.ee.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Resolution hints for a standard execution environment, e.g. "CDC-1.0/Foundation-1.0" or
 * "JavaSE-1.7"
 */
public final class StandardEEResolutionHints implements ExecutionEnvironmentResolutionHints {

    private final Map<VersionedId, IInstallableUnit> additionalUnits;
    private final Map<VersionedId, IInstallableUnit> temporaryUnits;
    private final ExecutionEnvironment executionEnvironment;

    public StandardEEResolutionHints(ExecutionEnvironment executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
        additionalUnits = computeAdditionalUnits(executionEnvironment);
        temporaryUnits = computeTemporaryAdditions(additionalUnits);
    }

    /**
     * p2 repositories are polluted with useless a.jre/config.a.jre IUs. These IUs do not represent
     * current/desired JRE and can expose resolver to packages that are not actually available.
     */
    @Override
    public boolean isNonApplicableEEUnit(IInstallableUnit iu) {
        // See JREAction
        return iu.getId().startsWith("a.jre") || iu.getId().startsWith("config.a.jre");
    }

    @Override
    public boolean isEESpecificationUnit(IInstallableUnit unit) {
        // not needed
        throw new UnsupportedOperationException();
    }

    /**
     * Return IUs that represent packages provided by target JRE
     *
     * @param executionEnvironment
     */
    private static Map<VersionedId, IInstallableUnit> computeAdditionalUnits(
            ExecutionEnvironment executionEnvironment) {
        Map<VersionedId, IInstallableUnit> units = new LinkedHashMap<>();
        addIUsFromEnvironment(executionEnvironment, units);
        return units;
    }

    static void addIUsFromEnvironment(ExecutionEnvironment executionEnvironment,
            Map<VersionedId, IInstallableUnit> units) {
        InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
        iu.setSingleton(false);
        String[] segements = executionEnvironment.getProfileName().split("-");
        iu.setId("a.jre." + segements[0].toLowerCase()); // using conventional name
        iu.setVersion(Version.create(segements[segements.length - 1]));
        iu.setTouchpointType(PublisherHelper.TOUCHPOINT_NATIVE);
        List<IProvidedCapability> capabilities = new ArrayList<>();
        capabilities.add(PublisherHelper.createSelfCapability(iu.getId(), iu.getVersion()));
        executionEnvironment.getSystemPackages().stream()
                .map(systemPackage -> MetadataFactory.createProvidedCapability(
                        PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE, systemPackage.packageName,
                        Version.create(systemPackage.version == null ? "0.0.0" : systemPackage.version)))
                .forEach(capabilities::add);
        String systemCapabilities = executionEnvironment.getProfileProperties()
                .getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES);
        capabilities.addAll(parseSystemCapabilities(systemCapabilities));
        iu.setCapabilities(capabilities.toArray(IProvidedCapability[]::new));
        // generate real IUs that represent requested execution environment
        put(units, MetadataFactory.createInstallableUnit(iu));
    }

    @Override
    public Collection<IInstallableUnit> getMandatoryUnits() {
        return additionalUnits.values();
    }

    @Override
    public Collection<IRequirement> getMandatoryRequires() {
        // not needed; getMandatoryUnits already enforces the use of the JRE IUs during resolution
        return Collections.emptyList();
    }

    private Map<VersionedId, IInstallableUnit> computeTemporaryAdditions(
            Map<VersionedId, IInstallableUnit> additionalUnits) {
        return Collections.emptyMap();
    }

    @Override
    public Collection<IInstallableUnit> getTemporaryAdditions() {
        return temporaryUnits.values();
    }

    private static void put(Map<VersionedId, IInstallableUnit> units, IInstallableUnit unit) {
        units.put(new VersionedId(unit.getId(), unit.getVersion()), unit);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(executionEnvironment);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || //
                (obj instanceof StandardEEResolutionHints other
                        && Objects.equals(executionEnvironment, other.executionEnvironment));
    }

    static Collection<IProvidedCapability> parseSystemCapabilities(String systemCapabilities) {
        if (systemCapabilities == null || systemCapabilities.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays
                    .stream(ManifestElement.parseHeader(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilities)) //
                    .flatMap(eeCapability -> {
                        String eeName = eeCapability.getAttribute("osgi.ee"); //$NON-NLS-1$
                        if (eeName == null) {
                            return Stream.empty();
                        }
                        return parseEECapabilityVersion(eeCapability) //
                                .map(version -> MetadataFactory.createProvidedCapability("osgi.ee", eeName, version)); //$NON-NLS-1$
                    }).toList();
        } catch (BundleException e) {
            return Collections.emptyList();
        }
    }

    private static Stream<Version> parseEECapabilityVersion(ManifestElement eeCapability) {
        String singleVersion = eeCapability.getAttribute("version:Version"); //$NON-NLS-1$
        String[] multipleVersions = ManifestElement
                .getArrayFromList(eeCapability.getAttribute("version:List<Version>")); //$NON-NLS-1$

        if (singleVersion == null && multipleVersions == null) {
            return Stream.empty();
        } else if (singleVersion == null) {
            return Arrays.stream(multipleVersions).map(Version::parseVersion);
        } else if (multipleVersions == null) {
            return Stream.of(singleVersion).map(Version::parseVersion);
        }
        return Stream.empty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StandardEEResolutionHints [executionEnvironment=");
        builder.append(executionEnvironment);
        builder.append("]");
        return builder.toString();
    }

}
