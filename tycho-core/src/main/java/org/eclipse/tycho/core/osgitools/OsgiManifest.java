package org.eclipse.tycho.core.osgitools;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_LOCALIZATION;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.core.UnknownEnvironmentException;
import org.eclipse.tycho.core.utils.ExecutionEnvironment;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Convenience wrapper around {@link Headers} and {@link ManifestElement} which adds typed getters
 * and value caching for commonly used headers. This is a read-only API.
 * 
 */
public class OsgiManifest {

    private static final ExecutionEnvironment[] EMPTY_EXEC_ENV = new ExecutionEnvironment[0];

    private String location;
    private Headers<String, String> headers;

    // cache for parsed values of commonly used headers
    private String bundleSymbolicName;
    private String bundleVersion;
    private String[] bundleClassPath;
    private ExecutionEnvironment[] executionEnvironments;
    private boolean isDirectoryShape;
    private Map<String, String> bundlePropFileMap;

    private OsgiManifest(InputStream stream, String location) throws OsgiManifestParserException {
        this.location = location;
        try {
            this.headers = Headers.parseManifest(stream);
        } catch (BundleException e) {
            throw new OsgiManifestParserException(location, e);
        }
        this.bundlePropFileMap = parseBundleLocalization();
        this.bundleSymbolicName = parseMandatoryFirstValue(BUNDLE_SYMBOLICNAME);
        this.bundleVersion = parseBundleVersion();
        this.bundleClassPath = parseBundleClasspath();
        this.isDirectoryShape = parseDirectoryShape();
        this.executionEnvironments = parseExecutionEnvironments();
        // TODO if we want more strict and OSGi-specific validation here, we could use
        // StateObjectFactory#createBundleDescription(State state, Dictionary<String, String> manifest, String location, long id) 
    }

    private ExecutionEnvironment[] parseExecutionEnvironments() {
        ManifestElement[] brees = getManifestElements(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (brees == null || brees.length == 0) {
            return EMPTY_EXEC_ENV;
        }
        ExecutionEnvironment[] envs = new ExecutionEnvironment[brees.length];
        try {
            for (int i = 0; i < brees.length; i++) {
                envs[i] = ExecutionEnvironmentUtils.getExecutionEnvironment(brees[i].getValue());
            }
        } catch (UnknownEnvironmentException e) {
            throw new OsgiManifestParserException(location, e);
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

    private Map<String, String> parseBundleLocalization() {
        String fileValue = headers.get(BUNDLE_LOCALIZATION);
        fileValue = (fileValue != null) ? fileValue : Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
        Properties properties = new Properties();
        Map<String, String> ret = new HashMap<String, String>();

        try {
            String dir = location.replace(JarFile.MANIFEST_NAME, "");
            FileInputStream ins = new FileInputStream(dir + fileValue + ".properties");
            properties.load(ins);
            ins.close();

            Enumeration<String> keys = headers.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = headers.get(key);
                // if the value is '%foo' we look for 'foo'
                if (value.startsWith("%")) {
                    String realValue = (String) properties.get(value.substring(1));
                    ret.put(key, realValue);
                }
            }
        } catch (FileNotFoundException e) {
            // no bundle localization used, or the file does not exist
            return null;
        } catch (IOException e) {
            return null;
        }
        return ret;
    }

    public Headers<String, String> getHeaders() {
        return headers;
    }

    public String getValue(String key) {
        if (bundlePropFileMap != null && bundlePropFileMap.containsKey(key)) {
            return bundlePropFileMap.get(key);
        }
        return headers.get(key);
    }

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public String[] getBundleClasspath() {
        return bundleClassPath;
    }

    public ExecutionEnvironment[] getExecutionEnvironments() {
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
