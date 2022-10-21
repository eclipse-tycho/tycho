package org.eclipse.tycho.test.util;

import java.net.URI;

import org.eclipse.tycho.core.test.utils.ResourceUtil;

/**
 * p2 repository resources used for multiple tests.
 */
public enum P2Repositories {
    ECLIPSE_342("e342"), PACK_GZ("packgz");

    private final String path;

    P2Repositories(String path) {
        this.path = path;
    }

    public URI toURI() {
        return ResourceUtil.resourceFile("repositories/" + path).toURI();

    }

    @Override
    public String toString() {
        return toURI().toString();
    }

}
