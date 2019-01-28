package org.eclipse.tycho.core.osgitools;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.internal.resolver.StateObjectFactoryImpl;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.StandardExecutionEnvironment;
import org.eclipse.tycho.core.ee.UnknownEnvironmentException;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Convenience wrapper around {@link Headers} and {@link ManifestElement} which adds typed getters
 * and value caching for commonly used headers. This is a read-only API.
 * 
 */
public class OsgiManifest {

    private static final StandardExecutionEnvironment[] EMPTY_EXEC_ENV = new StandardExecutionEnvironment[0];

    private URL location;
    private Headers<String, String> headers;

    // cache for parsed values of commonly used headers
    private String bundleSymbolicName;
    private String bundleVersion;
    private String[] bundleClassPath;
    private StandardExecutionEnvironment[] executionEnvironments;
    private boolean isDirectoryShape;

    private OsgiManifest(InputStream stream, URL location) throws OsgiManifestParserException {
        this.location = location;
        try {
            validateContent(location);

            this.headers = Headers.parseManifest(stream);
            // this will do more strict validation of headers on OSGi semantical level
            BundleDescription bundleDescription = StateObjectFactoryImpl.defaultFactory.createBundleDescription(null,
                    headers, location.toString(), 0L);
            this.bundleSymbolicName = bundleDescription.getSymbolicName();
        } catch (BundleException e) {
            throw new OsgiManifestParserException(location.toString(), e);
        }
        this.bundleVersion = parseBundleVersion();
        this.bundleClassPath = parseBundleClasspath();
        this.isDirectoryShape = parseDirectoryShape();
        this.executionEnvironments = parseExecutionEnvironments();
    }

    static void validateContent(URL location) throws OsgiManifestParserException {
        List<String> result = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(location.openStream(), Charset.forName("UTF-8"))) {
            StringBuffer sb = new StringBuffer();
            while (reader.ready()) {
                char c = (char) reader.read();
                if (c == '\n') {
                    result.add(sb.toString());
                    sb = new StringBuffer();
                } else {
                    sb.append(c);
                }
            }
            result.add(sb.toString() + "\n");

        } catch (Exception e) {
            throw new OsgiManifestParserException(location.toString(), e);
        }

        String lastLine = result.get(result.size() - 1);

        if (!lastLine.matches("\\s+")) {
            throw new OsgiManifestParserException(location.toString(), "Header must be terminated by a line break");
        }

    }

    private StandardExecutionEnvironment[] parseExecutionEnvironments() {
        ManifestElement[] brees = getManifestElements(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (brees == null || brees.length == 0) {
            return EMPTY_EXEC_ENV;
        }
        StandardExecutionEnvironment[] envs = new StandardExecutionEnvironment[brees.length];
        try {
            for (int i = 0; i < brees.length; i++) {
                envs[i] = ExecutionEnvironmentUtils.getExecutionEnvironment(brees[i].getValue());
            }
        } catch (UnknownEnvironmentException e) {
            throw new OsgiManifestParserException(location.toString(), e);
        }
        return envs;
    }

    private String parseBundleVersion() {
        String versionString = parseMandatoryFirstValue(BUNDLE_VERSION);
        try {
            return Version.parseVersion(versionString).toString();
        } catch (NumberFormatException e) {
            throw new InvalidOSGiManifestException(location.toString(),
                    "Bundle-Version '" + versionString + "' is invalid");
        } catch (IllegalArgumentException e) {
            throw new InvalidOSGiManifestException(location.toString(), e);
        }
    }

    private String parseMandatoryFirstValue(String headerKey) throws InvalidOSGiManifestException {
        String value = headers.get(headerKey);
        if (value == null) {
            throw new InvalidOSGiManifestException(location.toString(),
                    "MANIFEST header '" + headerKey + "' not found");
        }
        ManifestElement[] elements = null;
        try {
            elements = ManifestElement.parseHeader(headerKey, value);
        } catch (BundleException e) {
            throw new InvalidOSGiManifestException(location.toString(), e);
        }
        if (elements == null || elements.length == 0) {
            throw new InvalidOSGiManifestException(location.toString(),
                    "value for MANIFEST header '" + headerKey + "' is empty");
        }
        return elements[0].getValue();
    }

    private boolean parseDirectoryShape() {
        ManifestElement[] bundleShapeElements = parseHeader("Eclipse-BundleShape");
        return bundleShapeElements != null && bundleShapeElements.length > 0
                && "dir".equals(bundleShapeElements[0].getValue());
    }

    public Headers<String, String> getHeaders() {
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

    public StandardExecutionEnvironment[] getExecutionEnvironments() {
        return executionEnvironments;
    }

    /**
     * Returns true if Eclipse-BundleShape header is set to dir.
     * 
     * http://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/
     * bundle_manifest.html
     * 
     * http://eclipsesource.com/blogs/2009/01/20/tip-eclipse-bundleshape/
     */
    public boolean isDirectoryShape() {
        return isDirectoryShape;
    }

    static OsgiManifest parse(InputStream stream, URL location) throws OsgiManifestParserException {
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
            throw new OsgiManifestParserException(location.toString(), e);
        }
    }

    public ManifestElement[] getManifestElements(String key) throws OsgiManifestParserException {
        try {
            return ManifestElement.parseHeader(key, headers.get(key));
        } catch (BundleException e) {
            throw new OsgiManifestParserException(location.toString(), e);
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
