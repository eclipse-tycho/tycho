/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.shared;

/**
 * Handle on a unit which defines dependencies of a project. There may be more than one dependency
 * seed per project. The seeds are used for product and p2 repository assembly.
 */
public class DependencySeed {

    private Object installableUnit;

    public DependencySeed(Object installableUnit) {
        this.installableUnit = installableUnit;
    }

    public/* IInstallableUnit */Object getInstallableUnit() {
        return installableUnit;
    }

}
