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
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.RequiredCapability;
import org.eclipse.tycho.p2.resolver.facade.Requirements;

@SuppressWarnings("restriction")
public class P2Requirements implements Requirements {

    @Override
    public List<RequiredCapability> getRequiredCapabilities(IDependencyMetadata dependencyMetadata,
            DependencyMetadataType... types) {
        return dependencyMetadata.getDependencyMetadata(IRequiredCapability.class, types)
                .map(RequiredCapabilityFacade::new).collect(Collectors.toList());
    }

    private static final class RequiredCapabilityFacade implements RequiredCapability {

        private IRequiredCapability p2;

        public RequiredCapabilityFacade(IRequiredCapability p2) {
            this.p2 = p2;
        }

        @Override
        public String getId() {
            return p2.getName();
        }

        @Override
        public String getVersionRange() {
            return p2.getRange().toString();
        }

        @Override
        public String getNamespace() {
            return p2.getNamespace();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("RequiredCapability [Namespace=");
            builder.append(getNamespace());
            builder.append(", Id=");
            builder.append(getId());
            builder.append(", VersionRange=");
            builder.append(getVersionRange());
            builder.append("]");
            return builder.toString();
        }

    }

}
