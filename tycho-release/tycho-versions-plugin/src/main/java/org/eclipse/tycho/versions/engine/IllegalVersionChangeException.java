/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
