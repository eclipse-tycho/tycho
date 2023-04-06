/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper class around Xpp3Dom that uses reflection to ensure we don't run into stupid CCE. This
 * also give a nicer API to add new elements.
 */
public class MavenConfiguation {

    private Class<?> xpp3DomClass;
    private Object xpp3;

    MavenConfiguation(Object xpp3, String elementName) {
        ClassLoader loader = org.apache.maven.model.io.xpp3.MavenXpp3Writer.class.getClassLoader();
        try {
            xpp3DomClass = loader.loadClass("org.codehaus.plexus.util.xml.Xpp3Dom");
            if (xpp3 == null) {
                //config = new Xpp3Dom("configuration")
                xpp3 = xpp3DomClass.getConstructor(String.class).newInstance(elementName);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("can't create Xpp3Dom instance!", e);
        }
        this.xpp3 = xpp3;
    }

    public Object getXpp3() {
        return xpp3;
    }

    public MavenConfiguation addChild(String child) {
        MavenConfiguation childConfiguation = new MavenConfiguation(null, child);
        try {
            Method method = xpp3DomClass.getMethod("addChild", xpp3DomClass);
            method.invoke(getXpp3(), childConfiguation.getXpp3());
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException("can't add Xpp3Dom child!", e);
        }
        return childConfiguation;
    }

    public MavenConfiguation getChild(String child) {
        try {
            Method method = xpp3DomClass.getMethod("getChild", String.class);
            Object existing = method.invoke(getXpp3(), child);
            if (existing == null) {
                return addChild(child);
            }
            return new MavenConfiguation(existing, child);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException("can't add Xpp3Dom child!", e);
        }
    }

    public void setValue(String value) {
        try {
            Method method = xpp3DomClass.getMethod("setValue", String.class);
            method.invoke(getXpp3(), value);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException("can't add Xpp3Dom child!", e);
        }
    }

    @Override
    public String toString() {
        return getXpp3().toString();
    }

}
