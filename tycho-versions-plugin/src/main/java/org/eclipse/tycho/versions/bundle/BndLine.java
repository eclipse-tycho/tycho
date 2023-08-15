/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

class BndLine {

    String eol;
    String rawstring;
    BndLine nextline;
    String key;
    String value;
    String newValue;

    boolean isContinuation() {
        return rawstring.strip().endsWith("\\");
    }

    void parse() {
        Properties properties = new Properties();
        String collect = collect();
        try {
            properties.load(new StringReader(collect));
        } catch (IOException e) {
            throw new AssertionError("I/O error while reading a string!", e);
        }
        Set<String> names = properties.stringPropertyNames();
        if (names.isEmpty()) {
            return;
        }
        if (names.size() == 1) {
            this.key = names.iterator().next();
            this.value = properties.getProperty(key);
            return;
        }
        throw new AssertionError("Line yields more than one property: " + collect);
    }

    String collect() {
        if (nextline == null) {
            return rawstring;
        }
        return rawstring + nextline.collect();
    }

    @Override
    public String toString() {
        if (key == null) {
            return collect();
        }
        return key + ": " + value;
    }

}
