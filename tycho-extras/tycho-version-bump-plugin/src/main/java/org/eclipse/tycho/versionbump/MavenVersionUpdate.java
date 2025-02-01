/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
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
package org.eclipse.tycho.versionbump;

import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.core.MarkdownBuilder;

record MavenVersionUpdate(Artifact currentArtifact, String newVersion, IInstallableUnit current,
        IInstallableUnit update) {

    public void describeUpdate(MarkdownBuilder builder) {
        String msg = String.format("`%s` has been updated to version `%s`", currentArtifact().getId(), newVersion());
        builder.addListItem(msg);
        IInstallableUnit current = current();
        IInstallableUnit updated = update();
        if (current != null && updated != null) {
            Collection<IRequirement> currentRequirements = current.getRequirements();
            for (IRequirement requirement : update.getRequirements()) {
                if (!currentRequirements.contains(requirement) && requirement.getMin() > 0) {
                    builder.addListItem2(
                            String.format("additionally requires %s compared to the previous version", requirement));
                }
            }
        }
    }

    public String id() {
        return currentArtifact().getId() + "->" + newVersion();
    }
}
