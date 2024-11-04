package org.eclipse.tycho.core.osgitools;

import static org.osgi.framework.Constants.BUNDLE_CLASSPATH;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes.Name;

import org.eclipse.osgi.container.builders.OSGiManifestBuilderFactory;
import org.eclipse.osgi.container.namespaces.EclipsePlatformNamespace;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.TargetEnvironment;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
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
            if (location.endsWith(".bnd")) {
                Properties properties = new Properties();
                properties.load(stream);
                for (String key : properties.stringPropertyNames()) {
                    try {
                        //check if this is a valid manifest name...
                        new Name(key);
                    } catch (IllegalArgumentException e) {
                        // ... otherwise skip ...
                        continue;
                    }
                    headers.put(key, properties.getProperty(key));
                }
            } else {
                ManifestElement.parseBundleManifest(stream, headers);
            }
            // this will do more strict validation of headers on OSGi semantical level
            this.bundleSymbolicName = OSGiManifestBuilderFactory.createBuilder(headers).getSymbolicName();
        } catch (IOException | BundleException e) {
            throw new OsgiManifestParserException(location, e);
        }
        if (this.bundleSymbolicName == null) {
            throw new InvalidOSGiManifestException(location, "Bundle-SymbolicName is missing");
        }
        this.bundleVersion = parseBundleVersion();
        this.bundleClassPath = parseBundleClasspath();
        this.isDirectoryShape = parseDirectoryShape();
        this.executionEnvironments = parseExecutionEnvironments();
    }

    private String[] parseExecutionEnvironments() {
        @SuppressWarnings("deprecation")
        ManifestElement[] brees = getManifestElements(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (brees == null || brees.length == 0) {
            ManifestElement[] runee = getManifestElements(aQute.bnd.osgi.Constants.RUNEE);
            if (runee != null && runee.length > 0) {
                return elementToString(runee);
            }
            return EMPTY_EXEC_ENV;
        }
        return elementToString(brees);
    }

    private String[] elementToString(ManifestElement[] brees) {
        String[] envs = new String[brees.length];
        for (int i = 0; i < brees.length; i++) {
            envs[i] = brees[i].getValue();
        }
        return envs;
    }

    private String parseBundleVersion() {

        ManifestElement[] elements = parseHeader(BUNDLE_VERSION);
        if (elements != null) {
            for (ManifestElement element : elements) {
                String versionString = element.getValue();
                try {
                    return Version.parseVersion(versionString).toString();
                } catch (NumberFormatException e) {
                    throw new InvalidOSGiManifestException(location,
                            "Bundle-Version '" + versionString + "' is invalid");
                } catch (IllegalArgumentException e) {
                    throw new InvalidOSGiManifestException(location, e);
                }
            }
        }
        return Version.emptyVersion.toString();
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

    public String getLocation() {
        return location;
    }

    /**
     * Returns true if Eclipse-BundleShape header is set to dir.
     * 
     * https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/
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

    public Filter getTargetEnvironmentFilter() {
        String filterStr = getValue(EclipsePlatformNamespace.ECLIPSE_PLATFORM_FILTER_HEADER);
        if (filterStr != null) {
            try {
                return FrameworkUtil.createFilter(filterStr);
            } catch (InvalidSyntaxException e) {
                // at least we tried...
            }
        }
        return null;
    }

    public TargetEnvironment getImplicitTargetEnvironment() {
        String filterStr = getValue(EclipsePlatformNamespace.ECLIPSE_PLATFORM_FILTER_HEADER);
        if (filterStr != null) {
            try {
                FilterImpl filter = FilterImpl.newInstance(filterStr);

                String ws = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_WS));
                String os = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_OS));
                String arch = sn(filter.getPrimaryKeyValue(PlatformPropertiesUtils.OSGI_ARCH));

                // validate if os/ws/arch are not null and actually match the filter
                if (ws != null && os != null && arch != null) {
                    Map<String, String> properties = new HashMap<>();
                    properties.put(PlatformPropertiesUtils.OSGI_WS, ws);
                    properties.put(PlatformPropertiesUtils.OSGI_OS, os);
                    properties.put(PlatformPropertiesUtils.OSGI_ARCH, arch);

                    if (filter.matches(properties)) {
                        return new TargetEnvironment(os, ws, arch);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // at least we tried...
            }
        }
        return null;
    }

    private static String sn(String str) {
        if (str != null && !str.isBlank()) {
            return str;
        }
        return null;
    }

}
