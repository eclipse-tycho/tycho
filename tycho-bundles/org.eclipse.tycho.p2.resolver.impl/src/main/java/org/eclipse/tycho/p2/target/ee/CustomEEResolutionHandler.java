/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target.ee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.publisher.actions.JREAction;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfiguration;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.eclipse.tycho.p2.resolver.ExecutionEnvironmentResolutionHints;

class CustomEEResolutionHandler extends ExecutionEnvironmentResolutionHandler {

    private ExecutionEnvironmentConfiguration eeConfiguration;

    public CustomEEResolutionHandler(ExecutionEnvironmentConfiguration eeConfiguration) {
        super(new CustomEEResolutionHints(eeConfiguration.getProfileName()));
        this.eeConfiguration = eeConfiguration;
    }

    @Override
    public void readFullSpecification(Collection<IInstallableUnit> targetPlatformContent) {
        ExecutionEnvironmentResolutionHints profileUnitFilter = getResolutionHints();
        IInstallableUnit specificationUnit = findSpecificationUnit(targetPlatformContent, profileUnitFilter);

        List<SystemCapability> systemCapabilities = new ArrayList<SystemCapability>();

        // TODO test & refactor
        for (IProvidedCapability capability : specificationUnit.getProvidedCapabilities()) {
            if (JREAction.NAMESPACE_OSGI_EE.equals(capability.getNamespace())) {
                systemCapabilities.add(new SystemCapability(Type.OSGI_EE, capability.getName(), capability.getVersion()
                        .toString()));
            } else if (PublisherHelper.CAPABILITY_NS_JAVA_PACKAGE.equals(capability.getNamespace())) {
                systemCapabilities.add(new SystemCapability(Type.JAVA_PACKAGE, capability.getName(), capability
                        .getVersion().toString()));
            }
        }

        eeConfiguration.setFullSpecificationForCustomProfile(systemCapabilities);
    }

    private IInstallableUnit findSpecificationUnit(Collection<IInstallableUnit> targetPlatformContent,
            ExecutionEnvironmentResolutionHints profileUnitFilter) {
        IInstallableUnit specificationUnit;
        for (IInstallableUnit unit : targetPlatformContent) {
            if (profileUnitFilter.isEESpecificationUnit(unit))
                return unit;
        }
        // TODO throw exception
        return null;
    }

}
