package org.eclipse.tycho.core.resolver.shared;

public class ResolutionException extends Exception {

    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
