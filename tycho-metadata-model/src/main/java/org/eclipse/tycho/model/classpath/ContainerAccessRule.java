/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich  - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.model.classpath;

import java.util.Objects;

public interface ContainerAccessRule {

    public enum Kind {
        ACCESSIBLE("accessible"), NON_ACCESSIBLE("nonaccessible"), DISCOURAGED("discouraged"), UNKNOWN("");

        private String attribute;

        Kind(String attribute) {
            this.attribute = attribute;
        }

        static Kind parse(String value) {
            for (Kind kind : values()) {
                if (Objects.equals(kind.attribute, value)) {
                    return kind;
                }
            }
            return Kind.UNKNOWN;
        }

    }

    Kind getKind();

    String getPattern();
}
