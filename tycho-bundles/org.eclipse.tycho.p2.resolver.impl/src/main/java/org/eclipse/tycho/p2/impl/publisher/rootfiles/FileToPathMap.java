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
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

/**
 * A map using canonical files as keys and (relative) IPaths as values. Only canonicalized files are
 * used as keys internally.
 */
public class FileToPathMap {

    private HashMap<File, IPath> map;

    public FileToPathMap() {
        this.map = new HashMap<File, IPath>();
    }

    public void put(File key, IPath value) {
        map.put(canonify(key), value);
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
        try {
            return key.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int size() {
        return map.size();
    }

}
