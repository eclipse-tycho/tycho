/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class IllegalVersionChangeException extends IllegalArgumentException {

    private static final long serialVersionUID = -3988508831707189819L;

    private final List<String> errors;

    IllegalVersionChangeException(List<String> errors) {
        super(toString(errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public Collection<String> getErrors() {
        return errors;
    }

    private static String toString(List<String> errors) {
        StringBuilder sb = new StringBuilder("Invalid version: \n");
        for (String error : errors) {
            sb.append("  ").append(error).append("\n");
        }
        return sb.toString();
    }
}
