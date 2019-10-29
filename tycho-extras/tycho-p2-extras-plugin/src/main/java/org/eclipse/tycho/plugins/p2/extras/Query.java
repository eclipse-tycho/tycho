/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.plugins.p2.extras;

/**
 * Represents an IU MatchQuery.
 */
public class Query {

    private String expression;
    private String parameters;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String paramaters) {
        this.parameters = paramaters;
    }

    public String[] getParsedParameters() {
        if (parameters != null) {
            return parameters.split(",");
        } else {
            return new String[0];
        }
    }

}
