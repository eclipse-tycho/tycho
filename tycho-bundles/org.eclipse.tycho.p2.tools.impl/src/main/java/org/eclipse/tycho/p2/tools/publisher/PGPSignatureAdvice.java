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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

@SuppressWarnings("restriction")
public class PGPSignatureAdvice extends AbstractAdvice implements IPropertyAdvice {
    private final String id;
    private final Version version;
    private final String signature;
    private String publicKeys;

    public PGPSignatureAdvice(String id, Version version, String signature, String publicKeys) {
        this.id = id;
        this.version = version;
        this.signature = signature;
        this.publicKeys = publicKeys;
    }

    @Override
    protected String getId() {
        return id;
    }

    @Override
    protected Version getVersion() {
        return version;
    }

    @Override
    public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
        return null;
    }

    @Override
    public boolean isApplicable(String configSpec, boolean includeDefault, String candidateId,
            Version candidateVersion) {
        return id.equals(candidateId) && version.equals(candidateVersion);
    }

    @Override
    public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
        // workaround Bug 539672
        if (descriptor instanceof ArtifactDescriptor) {
            ArtifactDescriptor artifactDescriptor = (ArtifactDescriptor) descriptor;
            artifactDescriptor.setProperty("pgp.signatures", signature);
            if (publicKeys != null) {
                artifactDescriptor.setProperty("pgp.publicKeys", publicKeys);
            }
        }
        return null;
    }

}
