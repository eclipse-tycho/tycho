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
 * Basic implementation of the {@link BuildOrder.Import} interface.
 */
public class BuildOrderImport implements BuildOrder.Import {

    private final String namespace;
    private final String id;

    public BuildOrderImport(String namespace, String id) {
        this.namespace = namespace;
        this.id = id;

        if (namespace == null || id == null)
            throw new NullPointerException();
    }

    public boolean isSatisfiedBy(BuildOrder.Export export) {
        // TODO test this
        return namespace.equals(export.getNamespace()) && id.equals(export.getId());
    }

    @Override
    public String toString() {
        return "BuildOrder.Import(namespace=" + namespace + ", id=" + id + ")";
    }

}
