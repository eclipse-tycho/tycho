/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.publisher;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;

public class DependencySeedUtil {

    public static DependencySeed createSeed(String type, IInstallableUnit unit) {
        return new DependencySeed(type, unit.getId(), /* unit.getVersion().toString(), */unit);
    }

}
