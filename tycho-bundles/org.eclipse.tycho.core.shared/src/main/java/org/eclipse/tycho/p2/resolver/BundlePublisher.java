/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;

@SuppressWarnings("restriction")
public class BundlePublisher extends BundlesAction {

    private BundlePublisher(BundleDescription bundleDescription) {
        super(new BundleDescription[] { bundleDescription });
    }

    public static IInstallableUnit publishBundle(BundleDescription bundleDescription, IArtifactDescriptor descriptor,
            PublisherInfo publisherInfo) {
        BundlePublisher bundlesAction = new BundlePublisher(bundleDescription);
        bundlesAction.setPublisherInfo(publisherInfo);
        IInstallableUnit iu = bundlesAction.doCreateBundleIU(bundleDescription, descriptor.getArtifactKey(),
                publisherInfo);
        Collection<IPropertyAdvice> advice = publisherInfo.getAdvice(null, false, iu.getId(), iu.getVersion(),
                IPropertyAdvice.class);
        for (IPropertyAdvice entry : advice) {
            Map<String, String> props = entry.getArtifactProperties(iu, descriptor);
            if (props == null)
                continue;
            if (descriptor instanceof SimpleArtifactDescriptor simpleArtifactDescriptor) {
                for (Entry<String, String> pe : props.entrySet()) {
                    simpleArtifactDescriptor.setRepositoryProperty(pe.getKey(), pe.getValue());
                }
            }
        }
        return iu;
    }

}
