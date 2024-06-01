package org.eclipse.tycho.core.resolver;

import org.eclipse.tycho.BuildFailureException;

/**
 * Indicates, that the target platform configuration is invalid. E.g. if an element (ws, arch, os)
 * is missing
 */
public class TargetPlatformConfigurationException extends BuildFailureException {

    private static final long serialVersionUID = -8746373636075571516L;

    public TargetPlatformConfigurationException(String message) {
        super(message);
    }

    public TargetPlatformConfigurationException(String message, Throwable e) {
        super(message, e);
    }

}
