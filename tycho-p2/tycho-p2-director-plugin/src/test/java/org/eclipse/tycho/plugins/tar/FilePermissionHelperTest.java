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

import static java.util.Arrays.asList;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class FilePermissionHelperTest {

    @Test
    public void testOctalFileMode644() {
        Set<PosixFilePermission> perms = createPermissionSet(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
        Assert.assertEquals(0644, FilePermissionHelper.toOctalFileMode(perms));
    }

    @Test
    public void testOctalFileMode755() {
        Set<PosixFilePermission> perms = createPermissionSet(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);
        Assert.assertEquals(0755, FilePermissionHelper.toOctalFileMode(perms));
    }

    private Set<PosixFilePermission> createPermissionSet(PosixFilePermission... perms) {
        return new HashSet<PosixFilePermission>(asList(perms));
    }
}
