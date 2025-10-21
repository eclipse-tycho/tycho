/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultManifestHelper implements ManifestHelper {

    @Override
    public int getLineNumber(File manifestFile, String headerName) {
        if (manifestFile.isFile()) {
            try (FileInputStream stream = new FileInputStream(manifestFile)) {
                Iterator<String> iterator = Files.lines(manifestFile.toPath(), StandardCharsets.UTF_8).iterator();
                int number = 0;
                String search = headerName.toLowerCase() + ":";
                while (iterator.hasNext()) {
                    String line = iterator.next().toLowerCase();
                    number++;
                    if (line.startsWith(search)) {
                        return number;
                    }

                }
            } catch (IOException e) {
                // can' determine line number then...
            }
        }
        return 0;
    }

}
