/*******************************************************************************
 * Copyright (c) 2011, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Gunnar Wagenknecht (Salesforce) - fix for bug 509028
 *******************************************************************************/
package org.eclipse.tycho.repository.registry.facade;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class RepositoryBlackboardKey {

    // TODO p2 bug 347319 prevents using a special scheme, that will ensure that only our p2 repository factory load the blackboard key URIs
    //public static String SCHEME = "registry";
    public static final String SCHEME = "file";

    private final URI uri;

    private RepositoryBlackboardKey(URI uri) {
        this.uri = uri;
    }

    public URI toURI() {
        return uri;
    }

    /**
     * Creates a key under which the POM dependency artifacts of the resolution context (which is a
     * superset of the target platform) of a project are available as p2 repository.
     */
    public static RepositoryBlackboardKey forResolutionContextArtifacts(File projectLocation) {
        try {
            return new RepositoryBlackboardKey(new URI(SCHEME,
                    "/resolution-context-artifacts@" + URLEncoder.encode(String.valueOf(projectLocation), "UTF-8"),
                    null));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            // the used constructor does not escape invalid characters but we encode, so I don't see this happening (bug 509028)
        	// UTF-8 should be supported on any JVM
        	throw new RuntimeException(e);
        }
	
    }

    @Override
    public String toString() {
        return getClass().getName() + "(uri=" + uri + ")";
    }
}
