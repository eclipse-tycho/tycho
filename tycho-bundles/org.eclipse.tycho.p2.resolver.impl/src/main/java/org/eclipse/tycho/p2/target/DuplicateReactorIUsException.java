/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2.target;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class DuplicateReactorIUsException extends RuntimeException {
    private static final long serialVersionUID = 4233143438452165535L;

    private final Map<IInstallableUnit, Set<File>> duplicateReactorUIs;

    public DuplicateReactorIUsException(Map<IInstallableUnit, Set<File>> duplicateReactorUIs) {
        super(toString(duplicateReactorUIs));
        this.duplicateReactorUIs = duplicateReactorUIs;
    }

    private static String toString(Map<IInstallableUnit, Set<File>> duplicateReactorUIs) {
        StringBuilder sb = new StringBuilder("Duplicate reactor project IUs.\n");
        for (Map.Entry<IInstallableUnit, Set<File>> entry : duplicateReactorUIs.entrySet()) {
            IInstallableUnit iu = entry.getKey();
            Set<File> locations = entry.getValue();

            sb.append(iu.toString()).append(" => [");
            for (Iterator<File> locationIter = locations.iterator(); locationIter.hasNext();) {
                File location = locationIter.next();
                sb.append(location.getAbsolutePath());
                if (locationIter.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    public Map<IInstallableUnit, Set<File>> getDuplicateReactorUIs() {
        return duplicateReactorUIs;
    }
}
