/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher;

import java.util.Collections;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

@SuppressWarnings("restriction")
public class DownloadStatsAdvice implements IPropertyAdvice {

    static final String PROPERTY_NAME = "download.stats";

    @Override
    public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
        final StringBuilder builder = new StringBuilder();
        builder.append(iu.getId());
        builder.append('/');
        builder.append(iu.getVersion());
        // Workaround bug Bug 539672
        ((ArtifactDescriptor) descriptor).setProperty(PROPERTY_NAME, builder.toString());
        return null;
    }

    @Override
    public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
        return true;
    }

}
