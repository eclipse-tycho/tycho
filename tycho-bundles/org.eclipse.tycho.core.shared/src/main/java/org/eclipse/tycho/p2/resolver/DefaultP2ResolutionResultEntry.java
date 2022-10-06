/*******************************************************************************
 * Copyright (c) 2011, 2014 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

public class DefaultP2ResolutionResultEntry implements P2ResolutionResult.Entry {

    private String type;

    private String id;

    private String version;

    private Set<IInstallableUnit> installableUnits;

    private String classifier;

    private final Supplier<File> location;
    private File resolvedFile;

    public DefaultP2ResolutionResultEntry(String type, String id, String version, String classifier,
            Supplier<File> delayedLocation) {
        Objects.requireNonNull(delayedLocation);
        this.type = type;
        this.id = id;
        this.version = version;
        this.location = delayedLocation;
        this.classifier = classifier;
    }

    public DefaultP2ResolutionResultEntry(String type, String id, String version, String classifier,
            File resolvedLocation) {
        if (resolvedFile != null && !resolvedFile.exists()) {
            throw new IllegalArgumentException("File " + resolvedLocation + " does not exits!");
        }
        this.type = type;
        this.id = id;
        this.version = version;
        this.location = null;
        this.resolvedFile = resolvedLocation;
        this.classifier = classifier;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public File getLocation(boolean fetch) {
        if (fetch && (resolvedFile == null || !resolvedFile.exists()) && location != null) {
            resolvedFile = location.get();
        }
        return resolvedFile;
    }

    @Override
    public Set<IInstallableUnit> getInstallableUnits() {
        return installableUnits;
    }

    void addInstallableUnit(IInstallableUnit installableUnit) {
        if (installableUnits == null) {
            installableUnits = new LinkedHashSet<>();
        }
        installableUnits.add(installableUnit);
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DefaultP2ResolutionResultEntry [id=" + id + ", version=" + version + ", type=" + type + ", classifier="
                + classifier + ", location=" + location + ", resolvedFile=" + resolvedFile + "]";
    }

}
