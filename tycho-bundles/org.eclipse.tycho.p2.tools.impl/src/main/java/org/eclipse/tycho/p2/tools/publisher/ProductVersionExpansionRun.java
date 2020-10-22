/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
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
package org.eclipse.tycho.p2.tools.publisher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.artifacts.DependencyResolutionException;
import org.eclipse.tycho.artifacts.IllegalArtifactReferenceException;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.p2.target.P2TargetPlatform;

class ProductVersionExpansionRun {

    private final P2TargetPlatform targetPlatform;
    private final File productFile;

    StringBuilder errors = null;

    ProductVersionExpansionRun(P2TargetPlatform targetPlatform, File productFile) {
        this.targetPlatform = targetPlatform;
        this.productFile = productFile;
    }

    public List<IVersionedId> resolveReferences(String elementName, String artifactType, List<IVersionedId> references) {
        final List<IVersionedId> result = new ArrayList<>();
        for (IVersionedId reference : references) {
            IInstallableUnit resolvedUnit = resolveReferenceWithErrorHandling(elementName, artifactType, reference);
            if (resolvedUnit != null) {
                result.add(new VersionedId(reference.getId(), resolvedUnit.getVersion()));
            }
        }
        return result;
    }

    public List<IInstallableUnit> resolveReferencesToIUs(String elementName, String artifactType,
            List<IVersionedId> references) {
        final List<IInstallableUnit> result = new ArrayList<>();
        for (IVersionedId reference : references) {
            IInstallableUnit resolvedUnit = resolveReferenceWithErrorHandling(elementName, artifactType, reference);
            if (resolvedUnit != null) {
                result.add(resolvedUnit);
            }
        }
        return result;
    }

    private IInstallableUnit resolveReferenceWithErrorHandling(String elementName, String artifactType,
            IVersionedId reference) {
        try {
            return targetPlatform.resolveUnit(artifactType, reference.getId(), reference.getVersion());

        } catch (IllegalArtifactReferenceException e) {
            errors = initReferenceResolutionError(errors);
            errors.append("  Invalid <").append(elementName).append("> element with id=")
                    .append(quote(reference.getId()));
            if (reference.getVersion() != null) {
                errors.append(" and version=").append(quote(reference.getVersion()));
            }
            errors.append(": ").append(e.getMessage()).append('\n');
        } catch (DependencyResolutionException e) {
            errors = initReferenceResolutionError(errors);
            errors.append("  ").append(e.getMessage()).append('\n');
        }
        return null;
    }

    public void reportErrors(MavenLogger logger) {
        if (errors != null) {
            logger.error(errors.toString());
            throw new DependencyResolutionException("Cannot resolve dependencies of product " + productFile.getName()
                    + ". See log for details.");
        }
    }

    private StringBuilder initReferenceResolutionError(StringBuilder errors) {
        if (errors == null)
            return new StringBuilder("Cannot resolve dependencies of product " + productFile.getName() + ":\n");
        else
            return errors;
    }

    private static String quote(Object nullableObject) {
        if (nullableObject == null)
            return null;
        else
            return "\"" + nullableObject + "\"";
    }

}
