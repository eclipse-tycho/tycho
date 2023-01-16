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
package org.eclipse.tycho;

/**
 * An artifact key that carries additional information about the maven coordinates this one is
 * located.
 */
public interface MavenArtifactKey extends ArtifactKey {

    /**
     * @return the maven group id
     */
    String getGroupId();

    /**
     * @return the maven artifact id
     */
    String getArtifactId();

    static MavenArtifactKey bundle(String id, String version, String groupId, String artifactId) {
        return of(PackagingType.TYPE_ECLIPSE_PLUGIN, id, version, groupId, artifactId);
    }

    static MavenArtifactKey of(String type, String id, String version, String groupId, String artifactId) {
        return new MavenArtifactKey() {

            @Override
            public String getVersion() {
                return version;
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
            public String getGroupId() {
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Override
            public String toString() {
                return "MavenArtifactKey [" + type + " id=" + id + "] (" + groupId + ":" + artifactId
                        + ") with version " + version;
            }
        };
    }

}
