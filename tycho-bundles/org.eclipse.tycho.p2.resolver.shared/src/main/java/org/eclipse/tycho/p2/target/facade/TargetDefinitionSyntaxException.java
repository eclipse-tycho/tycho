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
package org.eclipse.tycho.p2.target.facade;

public class TargetDefinitionSyntaxException extends RuntimeException {

    public TargetDefinitionSyntaxException(String message) {
        super(message);
    }

    public TargetDefinitionSyntaxException(String message, Throwable e) {
        super(message, e);
    }
}
