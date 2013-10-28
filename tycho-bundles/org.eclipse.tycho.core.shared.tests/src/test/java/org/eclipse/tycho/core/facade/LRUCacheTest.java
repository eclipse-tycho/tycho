/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.facade;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class LRUCacheTest {

    @Test
    public void testMaxSize() {
        Map<String, String> cache = createPreFilledCacheSize5();
        cache.put("key6", "value6");
        assertEquals(5, cache.size());
    }

    @Test
    public void testRemoveEldestEntry() {
        Map<String, String> cache = createPreFilledCacheSize5();
        // use key1 to make it more recently used => key2 is eldest and must be removed instead 
        cache.get("key1");
        cache.put("key6", "value6");
        assertEquals(5, cache.size());
        assertEquals("value1", cache.get("key1"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
        assertEquals("value5", cache.get("key5"));
        assertEquals("value6", cache.get("key6"));
    }

    private Map<String, String> createPreFilledCacheSize5() {
        Map<String, String> cache = new LRUCache<String, String>(5);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");
        cache.put("key5", "value5");
        return cache;
    }

}
