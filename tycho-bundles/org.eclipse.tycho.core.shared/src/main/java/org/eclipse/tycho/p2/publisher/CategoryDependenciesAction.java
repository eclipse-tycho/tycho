/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher;

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
