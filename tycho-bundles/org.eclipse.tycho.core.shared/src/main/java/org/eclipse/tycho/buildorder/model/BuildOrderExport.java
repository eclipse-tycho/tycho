/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.model;

/**
 * Basic implementation of the {@link BuildOrder.Export} interface.
 */
public class BuildOrderExport implements BuildOrder.Export {
    private final String namespace;
    private final String id;

    public BuildOrderExport(String namespace, String id) {
        this.namespace = namespace;
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BuildOrder.Export(namespace=" + namespace + ", id=" + id + ")";
    }
}
