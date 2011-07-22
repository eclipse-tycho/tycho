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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;

public class FileToPathMapTest {

    private FileToPathMap map;

    @Before
    public void setup() {
        this.map = new FileToPathMap();
    }

    @Test
    public void testCanonifiedKeys() {
        map.put(new File("/tmp/test.txt"), new Path("test"));
        map.put(new File("/tmp/foo/../test.txt"), new Path("test2"));
        assertEquals(1, map.size());
        assertEquals(new Path("test2"), map.get(new File("/tmp/bar/../test.txt")));
    }

    @Test
    public void testPutAll() {
        map.put(new File("/tmp/test.txt"), new Path("test"));
        map.put(new File("/tmp/test2.txt"), new Path("test2"));
        FileToPathMap otherMap = new FileToPathMap();
        otherMap.putAll(map);
        assertEquals(2, otherMap.size());
        assertNotNull(otherMap.get(new File("/tmp/test.txt")));
        assertNotNull(otherMap.get(new File("/tmp/test2.txt")));
    }

}
