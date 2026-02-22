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
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.List;

import eu.maveniverse.domtrip.Element;

public class Property {
    private final Element dom;

    public Property(Element dom) {
        this.dom = dom;
    }

    public String getName() {
        return dom.name();
    }

    public void setValue(String value) {
        dom.textContent(value);
    }

    public static List<Property> getProperties(Element dom) {
        List<Property> result = new ArrayList<>();
        Element properties = dom.child("properties").orElse(null);
        if (properties != null) {
            for (Element property : properties.children().toList()) {
                result.add(new Property(property));
            }
        }
        return result;
    }

    public String getValue() {
        return dom.textContentTrimmed();
    }
}
