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
package org.eclipse.tycho.surefire.osgibooter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class CombinedClassLoader extends ClassLoader {

    private ClassLoader[] loaders;

    public CombinedClassLoader(ClassLoader... loaders) {
        this.loaders = loaders;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // try next
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> result = new ArrayList<URL>();
        for (ClassLoader loader : loaders) {
            Enumeration<URL> resources = loader.getResources(name);
            while (resources.hasMoreElements()) {
                result.add(resources.nextElement());
            }
        }
        return Collections.enumeration(result);
    }

    @Override
    protected URL findResource(String name) {
        URL url = null;
        for (ClassLoader loader : loaders) {
            url = loader.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
