/*******************************************************************************
 * Copyright (c) 2012, 2022 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich - Issue #797 - Implement a caching P2 transport  
 *******************************************************************************/
package org.eclipse.tycho;

import java.net.URI;

/**
 * Provides the mirror configuration and credentials from the Maven settings for loading remote p2
 * repositories.
 */
public interface MavenRepositorySettings {

    public final class Credentials {
        private final String userName;
        private final String password;
        private final URI url;

        public Credentials(String userName, String password, URI uri) {
            this.userName = userName;
            this.password = password;
            this.url = uri;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public URI getURI() {
            return url;
        }
    }

    /**
     * Returns the configured mirror URL, or <code>null</code>.
     */
    MavenRepositoryLocation getMirror(MavenRepositoryLocation location);

    /**
     * Returns the configured credentials for the given repository, or <code>null</code>.
     */
    Credentials getCredentials(MavenRepositoryLocation location);

}
