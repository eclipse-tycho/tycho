/*******************************************************************************
 * Copyright (c) 2014 IBH SYSTEMS GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBH SYSTEMS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.io.File;

final class SystemHelper {
    private SystemHelper() {
    }

    public static boolean isWindows() {
        return File.separatorChar == '\\'; // poor man's detection, from DefaultEquinoxLauncher
    }
}
