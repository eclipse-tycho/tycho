/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.osgitools;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.tycho.core.facade.BuildProperties;
import org.eclipse.tycho.core.facade.BuildPropertiesImpl;
import org.eclipse.tycho.core.facade.BuildPropertiesParser;
import org.eclipse.tycho.core.facade.LRUCache;

@Component(role = BuildPropertiesParser.class)
public class BuildPropertiesParserImpl implements BuildPropertiesParser, Disposable {

    private final LRUCache<String, BuildProperties> cache = new LRUCache<String, BuildProperties>(50);

    public BuildProperties parse(File baseDir) {
        try {
            File propsFile = new File(baseDir, BUILD_PROPERTIES);
            String filePath = propsFile.getCanonicalPath();
            BuildProperties buildProperties = cache.get(filePath);
            if (buildProperties == null) {
                buildProperties = new BuildPropertiesImpl(propsFile);
                cache.put(filePath, buildProperties);
            }
            return buildProperties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void dispose() {
        cache.clear();
    }

}
