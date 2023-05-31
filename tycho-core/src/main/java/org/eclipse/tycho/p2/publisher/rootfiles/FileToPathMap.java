/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher.rootfiles;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

/**
 * A map using normalized files as keys and (relative) IPaths as values. Only normalized files are
 * used as keys internally.
 */
public class FileToPathMap {

    private HashMap<File, IPath> map;

    public FileToPathMap() {
        this.map = new HashMap<>();
    }

    public void put(File key, IPath value) {
        map.put(canonify(key), value);
    }

    public Collection<IPath> values() {
        return map.values();
    }

    public IPath get(File key) {
        return map.get(canonify(key));
    }

    public void putAll(FileToPathMap otherMap) {
        for (Entry<File, IPath> entry : otherMap.map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set<File> keySet() {
        return map.keySet();
    }

    private static File canonify(File key) {
        return new File(key.getAbsoluteFile().toURI().normalize());
    }

    public int size() {
        return map.size();
    }

}
