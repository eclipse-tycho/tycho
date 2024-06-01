/*******************************************************************************
 * Copyright (c) 2011, 2023 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.test.util;

import java.io.Closeable;
import java.io.File;

import org.eclipse.tycho.FileLockService;

public class NoopFileLockService implements FileLockService {

    @Override
    public Closeable lock(File file, long timeout) {
        return lockVirtually(file);
    }

    @Override
    public Closeable lockVirtually(File file) {
        return () -> {
        };
    }

}
