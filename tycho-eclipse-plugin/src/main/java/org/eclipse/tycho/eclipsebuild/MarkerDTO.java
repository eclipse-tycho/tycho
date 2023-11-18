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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.eclipsebuild;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

class MarkerDTO implements IMarker, Serializable {

    private long id;
    private long creationTime;
    private String type;
    private String string;
    private HashMap<String, Object> attributes;
    private boolean exits;

    MarkerDTO(IMarker marker) {
        id = marker.getId();
        string = marker.toString();
        attributes = new HashMap<String, Object>();
        exits = marker.exists();
        try {
            creationTime = marker.getCreationTime();
        } catch (CoreException e) {
        }
        try {
            type = marker.getType();
        } catch (CoreException e) {
        }
        try {
            Map<String, Object> map = marker.getAttributes();
            if (map != null) {
                map.forEach((k, v) -> {
                    if (v == null) {
                        return;
                    }
                    attributes.put(k, v.toString());
                });
            }
        } catch (CoreException e) {
        }
    }

    @Override
    public Object getAttribute(String attributeName) throws CoreException {
        return getAttribute(attributeName, null);
    }

    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        try {
            return Integer.parseInt(getAttribute(attributeName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        return (String) attributes.getOrDefault(attributeName, defaultValue);
    }

    @Override
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        return Boolean.parseBoolean(getAttribute(attributeName, String.valueOf(defaultValue)));
    }

    @Override
    public Map<String, Object> getAttributes() throws CoreException {
        checkExsits();
        return attributes;
    }

    @Override
    public Object[] getAttributes(String[] attributeNames) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public long getCreationTime() throws CoreException {
        checkExsits();
        return creationTime;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getType() throws CoreException {
        checkExsits();
        return type;
    }

    @Override
    public IResource getResource() {
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public void delete() throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public boolean exists() {
        return exits;
    }

    @Override
    public boolean isSubtypeOf(String superType) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public void setAttribute(String attributeName, int value) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));

    }

    @Override
    public void setAttribute(String attributeName, Object value) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public void setAttribute(String attributeName, boolean value) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    @Override
    public void setAttributes(Map<String, ? extends Object> attributes) throws CoreException {
        throw new CoreException(Status.error("Not implemented"));
    }

    private void checkExsits() throws CoreException {
        if (!exits) {
            throw new CoreException(Status.error("DO not exits"));
        }

    }

    @Override
    public String toString() {
        return string;
    }
}
