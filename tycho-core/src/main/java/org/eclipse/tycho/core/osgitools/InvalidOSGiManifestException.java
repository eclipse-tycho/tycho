package org.eclipse.tycho.core.osgitools;

/**
 * Exception signaling an otherwise valid MANIFEST does not have valid mandatory OSGi headers
 * Bundle-SymbolicName or Bundle-Version.
 */
public class InvalidOSGiManifestException extends OsgiManifestParserException {

    private static final long serialVersionUID = -2887273472549676124L;

    public InvalidOSGiManifestException(String manifestLocation, String message) {
        super(manifestLocation, message);
    }

    public InvalidOSGiManifestException(String manifestLocation, Throwable cause) {
        super(manifestLocation, cause);
    }

}
