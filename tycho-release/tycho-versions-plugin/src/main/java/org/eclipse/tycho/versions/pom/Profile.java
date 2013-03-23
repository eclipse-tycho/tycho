/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import de.pdark.decentxml.Element;

public class Profile {

    private final Element dom;

    public Profile(Element dom) {
        this.dom = dom;
    }

    public List<String> getModules() {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Element modules : dom.getChildren("modules")) {
            for (Element module : modules.getChildren("module")) {
                result.add(module.getText());
            }
        }
        return new ArrayList<String>(result);
    }

    public String getId() {
        return dom.getChild("id").getText();
    }

    public Build getBuild() {
        return Build.getBuild(dom);
    }
}
