/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target.shared;

public record MavenRootDependency(String groupId, String artifactId, String version, String classifier, String type) {

    public String getType() {
        if (type == null || type.isBlank()) {
            return "jar";
        }
        return type;
    }

}
