package org.eclipse.tycho.core.osgitools;

/**
 * Exception signaling an invalid or non-existing OSGi manifest. It's unchecked because we generally
 * can't recover from an invalid OSGi manifest and want the build to be fail-fast.
 */
public class OsgiManifestParserException extends RuntimeException {

    private static final long serialVersionUID = -8673984213746219331L;

    public OsgiManifestParserException(String location, Throwable cause) {
        super("Exception parsing OSGi MANIFEST " + location + ": " + (cause != null ? cause.getMessage() : ""), cause);
    }

    public OsgiManifestParserException(String manifestLocation, String message) {
        super("Exception parsing OSGi MANIFEST " + manifestLocation + ": " + message);
    }

}
