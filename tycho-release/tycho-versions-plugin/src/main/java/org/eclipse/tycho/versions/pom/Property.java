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
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.List;

import de.pdark.decentxml.Element;

public class Property {
    private final Element dom;

    public Property(Element dom) {
        this.dom = dom;
    }

    public String getName() {
        return dom.getName();
    }

    public void setValue(String value) {
        dom.setText(value);
    }

    public static List<Property> getProperties(Element dom) {
        List<Property> result = new ArrayList<>();
        Element properties = dom.getChild("properties");
        if (properties != null) {
            for (Element property : properties.getChildren()) {
                result.add(new Property(property));
            }
        }
        return result;
    }

    public String getValue() {
        return dom.getTrimmedText();
    }
}
