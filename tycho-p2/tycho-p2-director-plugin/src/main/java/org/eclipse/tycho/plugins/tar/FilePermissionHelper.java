/*******************************************************************************
 * Copyright (c) 2014 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.tar;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FilePermissionHelper {

    private static final int OWNER_READ = 0400;
    private static final int OWNER_WRITE = 0200;
    private static final int OWNER_EXEC = 0100;
    private static final int GROUP_READ = 0040;
    private static final int GROUP_WRITE = 0020;
    private static final int GROUP_EXEC = 0010;
    private static final int OTHERS_READ = 0004;
    private static final int OTHERS_WRITE = 0002;
    private static final int OTHERS_EXEC = 0001;

    private FilePermissionHelper() {
    }

    public static int toOctalFileMode(Set<PosixFilePermission> permissions) {
        int result = 0;
        for (PosixFilePermission permissionBit : permissions) {
            switch (permissionBit) {
            case OWNER_READ:
                result |= OWNER_READ;
                break;
            case OWNER_WRITE:
                result |= OWNER_WRITE;
                break;
            case OWNER_EXECUTE:
                result |= OWNER_EXEC;
                break;
            case GROUP_READ:
                result |= GROUP_READ;
                break;
            case GROUP_WRITE:
                result |= GROUP_WRITE;
                break;
            case GROUP_EXECUTE:
                result |= GROUP_EXEC;
                break;
            case OTHERS_READ:
                result |= OTHERS_READ;
                break;
            case OTHERS_WRITE:
                result |= OTHERS_WRITE;
                break;
            case OTHERS_EXECUTE:
                result |= OTHERS_EXEC;
                break;
            }
        }
        return result;
    }

}
