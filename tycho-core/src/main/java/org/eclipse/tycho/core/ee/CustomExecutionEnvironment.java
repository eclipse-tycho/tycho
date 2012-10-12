/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tycho.core.ee.shared.ExecutionEnvironment;
import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.osgi.framework.Constants;

public class CustomExecutionEnvironment implements ExecutionEnvironment {
    private static final Pattern VERSION_NUMBER_DOT_NUMBER_DOT_ZERO_PATTERN = Pattern.compile("(\\d+\\.\\d+)\\.0");
    private static final Pattern JAVA_SECOND_EDITION_VERSIONS_PATTERN = Pattern.compile("(1\\.[0-5])");

    private final String profileName;
    private final Set<String> systemPackages = new LinkedHashSet<String>();
    private final Properties properties = new Properties();

    // BEGIN construction

    public CustomExecutionEnvironment(String profileName, List<SystemCapability> systemCapabilities) {
        this.profileName = profileName;
        setSystemPackages(systemCapabilities);
        setExecutionEnvironmentProperties(systemCapabilities);
        setOsgiSystemCapabilities(systemCapabilities);

        // osgi.java.profile.name is not needed at runtime AFAIK but let's make it explicit that this is a custom profile
        properties.setProperty(org.eclipse.osgi.framework.internal.core.Constants.OSGI_JAVA_PROFILE_NAME, profileName);
    }

    private void setSystemPackages(List<SystemCapability> systemCapabilities) {
        StringBuilder systemPackagesProperty = new StringBuilder();
        for (SystemCapability capability : systemCapabilities) {
            if (capability.getType() == Type.JAVA_PACKAGE) {
                final String packageName = capability.getName();
                systemPackages.add(packageName);
                append(systemPackagesProperty, toPackageSpecifier(packageName, capability.getVersion()));
            }
        }
        setPropertyIfNotEmpty(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackagesProperty);
    }

    private void setExecutionEnvironmentProperties(List<SystemCapability> systemCapabilities) {
        StringBuilder executionEnvironmentProperty = new StringBuilder();
        for (SystemCapability capability : systemCapabilities) {
            if (capability.getType() == Type.OSGI_EE) {
                String environmentName = capability.getName();
                String version = normalizeVersion(capability.getVersion());
                append(executionEnvironmentProperty, toExecutionEnvironment(environmentName, version));
            }
        }
        setPropertyIfNotEmpty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, executionEnvironmentProperty);
    }

    private static final class MultipleVersionsCapability {
        final String name;
        int count = 0;
        final StringBuilder versions = new StringBuilder();

        public MultipleVersionsCapability(String environmentName, String version) {
            name = environmentName;
            addVersion(version);
        }

        private void addVersion(String version) {
            if (count > 0) {
                versions.append(", ");
            }
            versions.append(version);
            count++;
        }

        @Override
        public String toString() {
            final StringBuilder result = new StringBuilder();
            result.append("osgi.ee; osgi.ee=\"" + name + "\"; ");
            if (count <= 1) {
                result.append("version:Version=\"");
            } else {
                result.append("version:List<Version>=\"");
            }
            result.append(versions);
            result.append("\"");
            return result.toString();
        }
    }

    private void setOsgiSystemCapabilities(List<SystemCapability> systemCapabilities) {
        Map<String, MultipleVersionsCapability> capabilityMap = new LinkedHashMap<String, MultipleVersionsCapability>();
        for (SystemCapability capability : systemCapabilities) {
            if (capability.getType() == Type.OSGI_EE) {
                String environmentName = capability.getName();
                String version = normalizeVersion(capability.getVersion());
                if (capabilityMap.containsKey(environmentName)) {
                    capabilityMap.get(environmentName).addVersion(version);
                } else {
                    capabilityMap.put(environmentName, new MultipleVersionsCapability(environmentName, version));
                }
            }
        }

        StringBuilder systemCapabilitesProperty = new StringBuilder();
        for (String environmentName : capabilityMap.keySet()) {
            MultipleVersionsCapability capability = capabilityMap.get(environmentName);
            append(systemCapabilitesProperty, capability.toString());
        }

        setPropertyIfNotEmpty(Constants.FRAMEWORK_SYSTEMCAPABILITIES, systemCapabilitesProperty);
    }

    private void setPropertyIfNotEmpty(String key, StringBuilder value) {
        if (value.length() > 0) {
            properties.setProperty(key, value.toString());
        }
    }

    private static void append(StringBuilder propertyValue, final String value) {
        if (propertyValue.length() > 0) {
            propertyValue.append(",");
        }
        propertyValue.append(value);
    }

    private String normalizeVersion(String version) {
        final Matcher matcher = VERSION_NUMBER_DOT_NUMBER_DOT_ZERO_PATTERN.matcher(version);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return version;
    }

    private String toExecutionEnvironment(String environmentName, String version) {
        if ("JavaSE".equals(environmentName)) {
            if (JAVA_SECOND_EDITION_VERSIONS_PATTERN.matcher(version).matches()) {
                return appendVersion("J2SE", version);
            }
        }
        if ("CDC/Foundation".equals(environmentName)) {
            return appendVersion("CDC", version) + "/" + appendVersion("Foundation", version);
        }
        return appendVersion(environmentName, version);
    }

    private String appendVersion(String value, String version) {
        return value + "-" + version;
    }

    private String toPackageSpecifier(String packageName, String packageVersion) {
        if (packageVersion != null) {
            return packageName + ";version=\"" + packageVersion + "\"";
        } else {
            return packageName;
        }
    }

    // END construction

    public String getProfileName() {
        return profileName;
    }

    public Properties getProfileProperties() {
        return properties;
    }

    public Set<String> getSystemPackages() {
        return systemPackages;
    }

    // not known for custom profiles
    public String getCompilerSourceLevel() {
        return null;
    }

    public String getCompilerTargetLevel() {
        return null;
    }

    public boolean isCompatibleCompilerTargetLevel(String target) {
        return true;
    }

    // for debug purposes
    @Override
    public String toString() {
        return "custom OSGi profile '" + getProfileName() + "'";
    }

}
