/*******************************************************************************
 * Copyright (c) 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.tar;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FilePermissionHelper {

    private static final int OWNER_READ_FILEMODE = 0400;
    private static final int OWNER_WRITE_FILEMODE = 0200;
    private static final int OWNER_EXEC_FILEMODE = 0100;
    private static final int GROUP_READ_FILEMODE = 0040;
    private static final int GROUP_WRITE_FILEMODE = 0020;
    private static final int GROUP_EXEC_FILEMODE = 0010;
    private static final int OTHERS_READ_FILEMODE = 0004;
    private static final int OTHERS_WRITE_FILEMODE = 0002;
    private static final int OTHERS_EXEC_FILEMODE = 0001;

    private FilePermissionHelper() {
    }

    /**
     * Converts a set of {@link PosixFilePermission} to chmod-style octal file mode.
     */
    public static int toOctalFileMode(Set<PosixFilePermission> permissions) {
        int result = 0;
        for (PosixFilePermission permissionBit : permissions) {
            switch (permissionBit) {
            case OWNER_READ:
                result |= OWNER_READ_FILEMODE;
                break;
            case OWNER_WRITE:
                result |= OWNER_WRITE_FILEMODE;
                break;
            case OWNER_EXECUTE:
                result |= OWNER_EXEC_FILEMODE;
                break;
            case GROUP_READ:
                result |= GROUP_READ_FILEMODE;
                break;
            case GROUP_WRITE:
                result |= GROUP_WRITE_FILEMODE;
                break;
            case GROUP_EXECUTE:
                result |= GROUP_EXEC_FILEMODE;
                break;
            case OTHERS_READ:
                result |= OTHERS_READ_FILEMODE;
                break;
            case OTHERS_WRITE:
                result |= OTHERS_WRITE_FILEMODE;
                break;
            case OTHERS_EXECUTE:
                result |= OTHERS_EXEC_FILEMODE;
                break;
            }
        }
        return result;
    }

}
