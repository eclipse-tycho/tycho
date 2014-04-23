package org.eclipse.tycho.plugins.tar;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FilePermissionHelper {

    private static final int OWNER_READ = 0400;/* Allows the owner to read */
    private static final int OWNER_WRITE = 0200; /* Allows the owner to write */
    private static final int OWNER_EXEC = 0100; /*
                                                 * Allows the owner to execute files and search in
                                                 * the directory
                                                 */
    private static final int GROUP_READ = 0040; /* Allows group members to read */
    private static final int GROUP_WRITE = 0020;/* Allows group members to write */
    private static final int GROUP_EXEC = 0010;/*
                                                * Allows group members to execute files and search
                                                * in the directory
                                                */
    private static final int OTHERS_READ = 0004;/* Allows everyone or the world to read */
    private static final int OTHERS_WRITE = 0002;/* Allows everyone or the world to write */
    private static final int OTHERS_EXEC = 0001; /*
                                                  * Allows everyone or the world to execute files
                                                  * and search in the directory
                                                  */

//    1000    Sets the sticky bit
//    2000    Sets the setgid bit
//    4000    Sets the setuid bit

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

    public static void main(String[] args) {
        System.out.println(Integer.toOctalString(toOctalFileMode(new HashSet<PosixFilePermission>(Arrays.asList(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, /*
                                                                                  * PosixFilePermission.
                                                                                  * OWNER_EXECUTE,
                                                                                  */
                PosixFilePermission.GROUP_READ, /* PosixFilePermission.GROUP_EXECUTE */PosixFilePermission.OTHERS_READ
        /* PosixFilePermission.OTHERS_EXECUTE */)))));
    }
}
