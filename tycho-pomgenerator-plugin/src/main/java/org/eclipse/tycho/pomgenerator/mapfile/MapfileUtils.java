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
package org.eclipse.tycho.pomgenerator.mapfile;

public class MapfileUtils {

    public static MapEntry parse(String line) {
        // some skip scenarios
        if (line == null) {
            return null;
        }
        if (line.trim().length() == 0) {
            return null;
        }
        if (line.startsWith("!")) {
            return null;
        }

        //plugin@org.eclipse.test=v20070226,:pserver:anonymous@dev.eclipse.org:/
        // cvsroot/eclipse,
        try {
            String[] nodes = line.split("=");
            String[] declarations = nodes[0].split("@");
            String kind = declarations[0];
            String name = declarations[1];

            String[] details = nodes[1].split(",");
            String version = details[0];
            String scmURL = details[1];

            String scmPath = null;
            if (details.length >= 4) {
                scmPath = details[3];
            }

            MapEntry entry = new MapEntry(kind, name, version, scmURL, scmPath);

            return entry;
        } catch (Throwable e) {
            throw new IllegalArgumentException("Invalid mapfile line: " + line + ". Unable to parse it!", e);
        }
    }

}
