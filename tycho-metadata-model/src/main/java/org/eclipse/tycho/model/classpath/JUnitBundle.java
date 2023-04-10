/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.tycho.model.classpath;

public interface JUnitBundle {

    String getBundleName();

    String getVersionRange();

    String getMavenGroupId();

    String getMavenArtifactId();

    static JUnitBundle of(String bundleName, String versionRange, String mavenGroupId, String mavenArtifactId) {
        return new JUnitBundle() {

            @Override
            public String getBundleName() {
                return bundleName;
            }

            @Override
            public String getVersionRange() {
                return versionRange;
            }

            @Override
            public String getMavenGroupId() {
                return mavenGroupId;
            }

            @Override
            public String getMavenArtifactId() {
                return mavenArtifactId;
            }

            @Override
            public String toString() {
                return "JUnit Bundle " + getBundleName() + " " + getVersionRange() + " (" + getMavenGroupId() + ":"
                        + getMavenArtifactId() + ":" + getVersionRange() + ")";
            }

        };
    }

}
