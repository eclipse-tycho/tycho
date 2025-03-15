/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

class SurefireLoader extends URLClassLoader implements BundleReference {

    private Bundle testBundle;
    private ClassLoader delegate;

    SurefireLoader(List<URL> urls, BundleClassLoader delegate) {
        super(urls.toArray(new URL[urls.size()]));
        this.delegate = delegate;
    }

    @Override
    public Bundle getBundle() {
        return testBundle;
    }

    @Override
    protected java.lang.Class<?> findClass(String name) throws ClassNotFoundException {
        if ("org.apache.maven.surefire.testng.utils.GroupMatcherMethodSelector".equals(name)) {
            //Surefire TestNGExecutor uses reflection to load this class and then calls reflective a static method to set the groups
            //TestNG itself then later uses classforname, so this specific class must *always* be loaded from
            //the OSGi loaders and never from our surefirebooter
            //as reflection is used this will not lead to class-space problems
            return delegate.loadClass(name);
        }
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            try {
                return delegate.loadClass(name);
            } catch (ClassNotFoundException delegate) {
                e.addSuppressed(delegate);
                throw e;
            }
        }
    }

    @Override
    public URL findResource(String name) {
        URL resource = super.findResource(name);
        if (resource == null) {
            return delegate.getResource(name);
        }
        return resource;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> resources = super.findResources(name);
        Enumeration<URL> resources2 = delegate.getResources(name);
        return new Enumeration<URL>() {

            @Override
            public URL nextElement() {
                if (resources.hasMoreElements()) {
                    return resources.nextElement();
                }
                if (resources2.hasMoreElements()) {
                    return resources2.nextElement();
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasMoreElements() {
                return resources.hasMoreElements() || resources2.hasMoreElements();
            }
        };
    }
}
