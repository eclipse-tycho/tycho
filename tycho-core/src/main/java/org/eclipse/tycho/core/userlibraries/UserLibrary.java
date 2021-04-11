/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.userlibraries;

import java.util.ArrayList;
import java.util.List;

public class UserLibrary {

    private String name;
    private List<String> pathList = new ArrayList<>();

    public UserLibrary(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UserLibrary [" + name + "]\r\n\t" + String.join("\r\n\t", pathList);
    }

    public void addPath(String path) {
        pathList.add(path);
    }

    public String getName() {
        return name;
    }

    public List<String> getPathList() {
        return pathList;
    }

}
