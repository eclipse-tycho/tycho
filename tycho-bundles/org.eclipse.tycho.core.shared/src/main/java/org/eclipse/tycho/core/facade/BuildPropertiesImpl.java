/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.facade;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class BuildPropertiesImpl implements BuildProperties {

    private String javacSource;
    private String javacTarget;
    private String jreCompilationProfile;
    private String forceContextQualifier;
    private boolean rootFilesUseDefaultExcludes;

    private List<String> binIncludes;
    private List<String> binExcludes;
    private List<String> sourceIncludes;
    private List<String> sourceExcludes;
    private List<String> jarsExtraClasspath;
    private List<String> jarsCompileOrder;

    private Map<String, List<String>> jarToSourceFolderMap;
    private Map<String, List<String>> jarToExtraClasspathMap;
    private Map<String, String> jarToJavacDefaultEncodingMap;
    private Map<String, String> jarToOutputFolderMap;
    private Map<String, String> rootEntries;

    public BuildPropertiesImpl(File propsFile) {
        this(readProperties(propsFile));
    }

    @SuppressWarnings("unchecked")
    public BuildPropertiesImpl(Properties properties) {
        javacSource = safeTrimValue("javacSource", properties);
        javacTarget = safeTrimValue("javacTarget", properties);
        forceContextQualifier = safeTrimValue("forceContextQualifier", properties);
        jreCompilationProfile = safeTrimValue("jre.compilation.profile", properties);
        rootFilesUseDefaultExcludes = Boolean.parseBoolean(properties.getProperty("rootFiles.useDefaultExcludes",
                "true"));

        sourceIncludes = splitAndTrimCommaSeparated(properties.getProperty("src.includes"));
        sourceExcludes = splitAndTrimCommaSeparated(properties.getProperty("src.excludes"));
        binIncludes = splitAndTrimCommaSeparated(properties.getProperty("bin.includes"));
        binExcludes = splitAndTrimCommaSeparated(properties.getProperty("bin.excludes"));
        jarsExtraClasspath = splitAndTrimCommaSeparated(properties.getProperty("jars.extra.classpath"));
        jarsCompileOrder = splitAndTrimCommaSeparated(properties.getProperty("jars.compile.order"));

        HashMap<String, List<String>> jarTosourceFolderTmp = new HashMap<String, List<String>>();
        HashMap<String, List<String>> jarToExtraClasspathTmp = new HashMap<String, List<String>>();
        HashMap<String, String> jarToJavacDefaultEncodingTmp = new HashMap<String, String>();
        HashMap<String, String> jarToOutputFolderMapTmp = new HashMap<String, String>();
        HashMap<String, String> rootEntriesTmp = new HashMap<String, String>();

        for (Entry<Object, Object> entry : properties.entrySet()) {
            String key = ((String) entry.getKey()).trim();
            String value = ((String) entry.getValue()).trim();
            if (key.startsWith("source.")) {
                String jarName = key.substring("source.".length());
                jarTosourceFolderTmp.put(jarName, splitAndTrimCommaSeparated(value));
            } else if (key.startsWith("extra.")) {
                String jarName = key.substring("extra.".length());
                jarToExtraClasspathTmp.put(jarName, splitAndTrimCommaSeparated(value));
            } else if (key.startsWith("javacDefaultEncoding.")) {
                String jarName = key.substring("javacDefaultEncoding.".length());
                jarToJavacDefaultEncodingTmp.put(jarName, value);
            } else if (key.startsWith("output.")) {
                String jarName = key.substring("output.".length());
                jarToOutputFolderMapTmp.put(jarName, value);
            } else if (key.startsWith("root.") || key.equals("root")) {
                rootEntriesTmp.put(key, value);
            }
        }
        jarToSourceFolderMap = unmodifiableMap(jarTosourceFolderTmp);
        jarToExtraClasspathMap = unmodifiableMap(jarToExtraClasspathTmp);
        jarToJavacDefaultEncodingMap = unmodifiableMap(jarToJavacDefaultEncodingTmp);
        jarToOutputFolderMap = unmodifiableMap(jarToOutputFolderMapTmp);
        rootEntries = unmodifiableMap(rootEntriesTmp);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map unmodifiableMap(Map map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }

    private static Properties readProperties(File propsFile) {
        Properties properties = new Properties();
        if (propsFile.canRead()) {
            // TODO should we fail the build if build.properties is missing?
            InputStream is = null;
            try {
                try {
                    is = new FileInputStream(propsFile);
                    properties.load(is);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return properties;
    }

    private static String safeTrimValue(String key, Properties buildProperties) {
        String value = buildProperties.getProperty(key);
        if (value != null) {
            value = value.trim();
        }
        return value;
    }

    private static List<String> splitAndTrimCommaSeparated(String rawValue) {
        List<String> result = new ArrayList<String>();
        if (rawValue != null) {
            for (String element : rawValue.split(",")) {
                result.add(element.trim());
            }
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> getBinIncludes() {
        return binIncludes;
    }

    public List<String> getBinExcludes() {
        return binExcludes;
    }

    public List<String> getSourceIncludes() {
        return sourceIncludes;
    }

    public List<String> getSourceExcludes() {
        return sourceExcludes;
    }

    public Map<String, List<String>> getJarToSourceFolderMap() {
        return jarToSourceFolderMap;
    }

    public List<String> getJarsExtraClasspath() {
        return jarsExtraClasspath;
    }

    public String getJavacSource() {
        return javacSource;
    }

    public String getJavacTarget() {
        return javacTarget;
    }

    public List<String> getJarsCompileOrder() {
        return jarsCompileOrder;
    }

    public Map<String, List<String>> getJarToExtraClasspathMap() {
        return jarToExtraClasspathMap;
    }

    public Map<String, String> getJarToJavacDefaultEncodingMap() {
        return jarToJavacDefaultEncodingMap;
    }

    public Map<String, String> getJarToOutputFolderMap() {
        return jarToOutputFolderMap;
    }

    public String getJreCompilationProfile() {
        return jreCompilationProfile;
    }

    public String getForceContextQualifier() {
        return forceContextQualifier;
    }

    public boolean isRootFilesUseDefaultExcludes() {
        return rootFilesUseDefaultExcludes;
    }

    public Map<String, String> getRootEntries() {
        return rootEntries;
    }

}
