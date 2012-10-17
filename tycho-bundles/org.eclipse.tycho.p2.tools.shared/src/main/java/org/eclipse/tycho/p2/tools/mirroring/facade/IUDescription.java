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
package org.eclipse.tycho.p2.tools.mirroring.facade;

import java.util.Arrays;

public class IUDescription {

    private String id;
    private String version;
    private String queryMatchExpression;
    private String[] queryParameters;

    /**
     * @param id
     *            id of the installable unit.
     * @param version
     *            version of the installable unit. <code>null</code> means latest available version.
     */
    public IUDescription(String id, String version) {
        this(id, version, null, null);
    }

    public IUDescription(String id, String version, String queryMatchExpression, String[] queryParameters) {
        if (id == null && queryMatchExpression == null) {
            throw new NullPointerException("either id or query must be specified");
        }
        this.id = id;
        this.version = version;
        this.queryMatchExpression = queryMatchExpression;
        this.queryParameters = queryParameters;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        if (queryMatchExpression != null) {
            return "[query expression='" + queryMatchExpression + ", parameters=" + Arrays.asList(queryParameters)
                    + "]";
        } else {
            return "[" + id + ", " + (version != null ? version : "0.0.0") + "]";
        }
    }

    public String getQueryMatchExpression() {
        return queryMatchExpression;
    }

    public String[] getQueryParameters() {
        return queryParameters;
    }

}
