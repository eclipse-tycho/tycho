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

import java.util.List;

import org.eclipse.tycho.buildorder.model.BuildOrder.Export;
import org.eclipse.tycho.buildorder.model.BuildOrder.Import;

public class BuildOrderRelations {
    private List<Import> imports;
    private List<Export> exports;

    public BuildOrderRelations(List<Import> imports, List<Export> exports) {
        this.imports = imports;
        this.exports = exports;
    }

    public List<BuildOrder.Import> getImports() {
        return imports;
    }

    public List<BuildOrder.Export> getExports() {
        return exports;
    }

}
