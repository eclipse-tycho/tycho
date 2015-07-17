/*******************************************************************************
 * Copyright (c) 2011, 2014 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

public class DefaultP2ResolutionResultEntry implements P2ResolutionResult.Entry {
    private String type;

    private String id;

    private String version;

    private final File location;

    private Set<Object> installableUnits;

    private final String classifier;

    public DefaultP2ResolutionResultEntry(String type, String id, String version, File location, String classifier) {
        this.type = type;
        this.id = id;
        this.version = version;
        this.location = location;
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
    public File getLocation() {
        return location;
    }

    @Override
    public Set<Object> getInstallableUnits() {
        return installableUnits;
    }

    void addInstallableUnit(Object installableUnit) {
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

}
