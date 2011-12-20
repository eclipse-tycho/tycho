/*******************************************************************************
 * Copyright (c) 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;

public class DefaultP2ResolutionResultEntry implements P2ResolutionResult.Entry {
    private final String type;

    private final String id;

    private final String version;

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

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public File getLocation() {
        return location;
    }

    public Set<Object> getInstallableUnits() {
        return installableUnits;
    }

    void addInstallableUnit(Object installableUnit) {
        if (installableUnits == null) {
            installableUnits = new LinkedHashSet<Object>();
        }
        installableUnits.add(installableUnit);
    }

    public String getClassifier() {
        return classifier;
    }

}
