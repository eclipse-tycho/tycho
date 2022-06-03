package org.eclipse.tycho.core.osgitools;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Convenience wrapper around {@link Headers} and {@link ManifestElement} which adds typed getters
 * and value caching for commonly used headers. This is a read-only API.
 * 
 */
public class OsgiManifest {

    private static final String[] EMPTY_EXEC_ENV = new String[0];

    private final String location;
    private final CaseInsensitiveDictionaryMap<String, String> headers;

    // cache for parsed values of commonly used headers
    private final String bundleSymbolicName;
    private final String bundleVersion;
    private final String[] bundleClassPath;
    private final String[] executionEnvironments;
    private final boolean isDirectoryShape;

    private OsgiManifest(InputStream stream, String location) throws OsgiManifestParserException {
        this.location = location;
        try {
            this.headers = new CaseInsensitiveDictionaryMap<>();
            ManifestElement.parseBundleManifest(stream, headers);
            // this will do more strict validation of headers on OSGi semantical level
            this.bundleSymbolicName = OSGiManifestBuilderFactory.createBuilder(headers).getSymbolicName();
        } catch (IOException | BundleException e) {
            throw new OsgiManifestParserException(location, e);
        }
        this.bundleVersion = parseBundleVersion();
        this.bundleClassPath = parseBundleClasspath();
        this.isDirectoryShape = parseDirectoryShape();
        this.executionEnvironments = parseExecutionEnvironments();
    }

    private String[] parseExecutionEnvironments() {
        ManifestElement[] brees = getManifestElements(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (brees == null || brees.length == 0) {
            return EMPTY_EXEC_ENV;
        }
        String[] envs = new String[brees.length];
        for (int i = 0; i < brees.length; i++) {
            //BREE already has no real meaning for modular vms so matching them here does not really offer much...
            envs[i] = brees[i].getValue();
        }
        return envs;
    }

    private String parseBundleVersion() {
        String versionString = parseMandatoryFirstValue(BUNDLE_VERSION);
        try {
            return Version.parseVersion(versionString).toString();
        } catch (NumberFormatException e) {
            throw new InvalidOSGiManifestException(location, "Bundle-Version '" + versionString + "' is invalid");
        } catch (IllegalArgumentException e) {
            throw new InvalidOSGiManifestException(location, e);
        }
    }

    private String parseMandatoryFirstValue(String headerKey) throws InvalidOSGiManifestException {
        String value = headers.get(headerKey);
        if (value == null) {
            throw new InvalidOSGiManifestException(location, "MANIFEST header '" + headerKey + "' not found");
        }
        ManifestElement[] elements = null;
        try {
            elements = ManifestElement.parseHeader(headerKey, value);
        } catch (BundleException e) {
            throw new InvalidOSGiManifestException(location, e);
        }
        if (elements == null || elements.length == 0) {
            throw new InvalidOSGiManifestException(location, "value for MANIFEST header '" + headerKey + "' is empty");
        }
        return elements[0].getValue();
    }

    private boolean parseDirectoryShape() {
        ManifestElement[] bundleShapeElements = parseHeader("Eclipse-BundleShape");
        return bundleShapeElements != null && bundleShapeElements.length > 0
                && "dir".equals(bundleShapeElements[0].getValue());
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getValue(String key) {
        return headers.get(key);
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    /**
     * Returns the bundle's key in the Eclipse artifact coordinate system.
     */
    public ArtifactKey toArtifactKey() {
        return new DefaultArtifactKey(ArtifactType.TYPE_ECLIPSE_PLUGIN, getBundleSymbolicName(), getBundleVersion());
    }

    public String[] getBundleClasspath() {
        return bundleClassPath;
    }

    public String[] getExecutionEnvironments() {
        return executionEnvironments;
    }

    /**
     * Returns true if Eclipse-BundleShape header is set to dir.
     * 
     * https://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/
     * bundle_manifest.html
     * 
     * https://eclipsesource.com/blogs/2009/01/20/tip-eclipse-bundleshape/
     */
    public boolean isDirectoryShape() {
        return isDirectoryShape;
    }

    static OsgiManifest parse(InputStream stream, String location) throws OsgiManifestParserException {
        return new OsgiManifest(stream, location);
    }

    private ManifestElement[] parseHeader(String key) {
        String value = headers.get(key);
        if (value == null) {
            return null;
        }
        try {
            return ManifestElement.parseHeader(key, value);
        } catch (BundleException e) {
            throw new OsgiManifestParserException(location, e);
        }
    }

    public ManifestElement[] getManifestElements(String key) throws OsgiManifestParserException {
        try {
            return ManifestElement.parseHeader(key, headers.get(key));
        } catch (BundleException e) {
            throw new OsgiManifestParserException(location, e);
        }
    }

    private String[] parseBundleClasspath() {
        String[] result = new String[] { "." };
        String classPathValue = getValue(BUNDLE_CLASSPATH);
        if (classPathValue != null) {
            ManifestElement[] classpathEntries = getManifestElements(BUNDLE_CLASSPATH);
            if (classpathEntries != null) {
                result = new String[classpathEntries.length];
                for (int i = 0; i < classpathEntries.length; i++) {
                    result[i] = classpathEntries[i].getValue();
                }
            }
        }
        return result;
    }

}
