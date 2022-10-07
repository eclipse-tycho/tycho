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
package org.eclipse.tycho.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

@SuppressWarnings("restriction")
public class MavenChecksumAdvice implements IPropertyAdvice {
    //We use a fixed list here of default supported ones, actually one can provide a ChecksumAlgorithmFactory to extend the default algorithms
    Map<String, String> mavenExtensions = Map.of(//
            ".md5", "download.checksum.md5", //
            ".sha1", "download.checksum.sha-1", //
            ".sha256", "download.checksum.sha-256", //
            ".sha512", "download.checksum.sha-512" //
    );

    private File artifactFile;

    public MavenChecksumAdvice(File artifactFile) {
        this.artifactFile = artifactFile;
    }

    @Override
    public Map<String, String> getInstallableUnitProperties(InstallableUnitDescription iu) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getArtifactProperties(IInstallableUnit iu, IArtifactDescriptor descriptor) {
        if (descriptor instanceof ArtifactDescriptor artifactDescriptor) {
            // Workaround bug Bug 539672
            String baseName = artifactFile.getName();
            for (var entry : mavenExtensions.entrySet()) {
                File file = new File(artifactFile.getParentFile(), baseName + entry.getKey());
                if (file.isFile()) {
                    try {
                        String checksum = Files.readString(file.toPath(), StandardCharsets.US_ASCII).strip();
                        artifactDescriptor.setProperty(entry.getValue(), checksum);
                    } catch (IOException e) {
                        //can't use the checksum then...
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
        return true;
    }

}
