/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.ExecutionEnvironmentResolutionHints;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;

class CustomEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    private ExecutionEnvironmentConfiguration eeConfiguration;

    public CustomEEResolutionHandler(ExecutionEnvironmentConfiguration eeConfiguration) {
        super(new CustomEEResolutionHints(eeConfiguration.getProfileName()));
        this.eeConfiguration = eeConfiguration;
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        IInstallableUnit specificationUnit = findSpecificationUnit(targetPlatformContent, getResolutionHints());
        List<SystemCapability> systemCapabilities = readCapabilities(specificationUnit);
        eeConfiguration.setFullSpecificationForCustomProfile(systemCapabilities);
    }

    private IInstallableUnit findSpecificationUnit(Collection<IInstallableUnit> targetPlatformContent,
            ExecutionEnvironmentResolutionHints profileUnitFilter) {
        for (IInstallableUnit unit : targetPlatformContent) {
            if (profileUnitFilter.isEESpecificationUnit(unit))
                return unit;
        }
        throw new IllegalArgumentException("Could not find specification for custom execution environment profile '"
                + eeConfiguration.getProfileName() + "' in the target platform");
        // TODO include IU name?
    }

    private List<SystemCapability> readCapabilities(IInstallableUnit specificationUnit) {
        List<SystemCapability> result = new ArrayList<>();

        for (IProvidedCapability capability : specificationUnit.getProvidedCapabilities()) {
            String namespace = capability.getNamespace();
            String name = capability.getName();
            String version = capability.getVersion().toString();

            if (JREAction.NAMESPACE_OSGI_EE.equals(namespace)) {
                result.add(new SystemCapability(Type.OSGI_EE, name, version));
            } else if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(namespace)) {
                result.add(new SystemCapability(Type.JAVA_PACKAGE, name, version));
            } else {
                // ignore
            }
        }
        return result;
    }

}
