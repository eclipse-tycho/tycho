package org.eclipse.tycho.core.resolver;

/**
 * Indicates, that the target platform configuration is invalid. E.g. if an element (ws, arch, os)
 * is missing
 */
public class TargetPlatformConfigurationException extends Exception {

    private static final long serialVersionUID = -8746373636075571516L;

    public TargetPlatformConfigurationException(String message) {
        super(message);
    }

}
