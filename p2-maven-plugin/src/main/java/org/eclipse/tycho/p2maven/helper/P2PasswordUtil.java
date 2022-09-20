/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.p2maven.helper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

public class P2PasswordUtil {

	public static void setCredentials(URI location, String username, String password, Logger logger) {
        ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

        // if URI is not opaque, just getting the host may be enough
        String host = location.getHost();
        if (host == null) {
            String scheme = location.getScheme();
            if (URIUtil.isFileURI(location) || scheme == null) {
                // If the URI references a file, a password could possibly be needed for the directory
                // (it could be a protected zip file representing a compressed directory) - in this
                // case the key is the path without the last segment.
                // Using "Path" this way may result in an empty string - which later will result in
                // an invalid key.
                host = new Path(location.toString()).removeLastSegments(1).toString();
            } else {
                // it is an opaque URI - details are unknown - can only use entire string.
                host = location.toString();
            }
        }
        String nodeKey = URLEncoder.encode(host, StandardCharsets.UTF_8);
        String nodeName = IRepository.PREFERENCE_NODE + '/' + nodeKey;

        ISecurePreferences prefNode = securePreferences.node(nodeName);

        try {
            if (!username.equals(prefNode.get(IRepository.PROP_USERNAME, username))
                    || !password.equals(prefNode.get(IRepository.PROP_PASSWORD, password))) {
                logger.info("Redefining access credentials for repository host " + host);
            }
            prefNode.put(IRepository.PROP_USERNAME, username, false);
            prefNode.put(IRepository.PROP_PASSWORD, password, false);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

}
