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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class NoParentPomFound extends FileNotFoundException {

    public NoParentPomFound(Path path) throws IOException {
        super("No parent pom file found in " + path.toAbsolutePath().toString());
    }

}
