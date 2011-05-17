/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.publisher;

import org.eclipse.equinox.internal.p2.updatesite.SiteModel;

@SuppressWarnings("restriction")
public class CategoryDependenciesAction extends AbstractSiteDependenciesAction {
    private final SiteModel siteModel;

    public CategoryDependenciesAction(SiteModel siteModel, String id, String version) {
        super(id, version);
        this.siteModel = siteModel;
    }

    @Override
    SiteModel getSiteModel() {
        return this.siteModel;
    }

}
