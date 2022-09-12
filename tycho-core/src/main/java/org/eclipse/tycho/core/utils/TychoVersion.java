/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TychoVersion {

    private static final String TYCHO_VERSION = readVersion();

    public static String getTychoVersion() {
        return TYCHO_VERSION;
    }

    private static String readVersion() {
        try (InputStream stream = TychoVersion.class.getResourceAsStream("version.properties")) {
            Properties p = new Properties();
            p.load(stream);
            return p.getProperty("version");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
