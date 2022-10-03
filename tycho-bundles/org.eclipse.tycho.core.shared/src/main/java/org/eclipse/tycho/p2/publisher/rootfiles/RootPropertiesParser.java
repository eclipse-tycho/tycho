/*******************************************************************************
 * Copyright (c) 2010, 2017 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Bachmann electronic GmbH - adding support for root.folder and root.<config>.folder
 *******************************************************************************/
package org.eclipse.tycho.p2.publisher.rootfiles;

import static org.eclipse.tycho.p2.publisher.rootfiles.SegmentHelper.segmentEquals;
import static org.eclipse.tycho.p2.publisher.rootfiles.SegmentHelper.segmentEqualsOrIsEndSegment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.tycho.BuildProperties;

public class RootPropertiesParser {
    static class ParsingResult {
        private HashMap<ConfigSpec, RootFilesProperties> propertiesPerConfig = new HashMap<>();

        RootFilesProperties getPropertiesForWriting(ConfigSpec config) {
            RootFilesProperties properties = propertiesPerConfig.get(config);
            if (properties == null) {
                properties = new RootFilesProperties();
                propertiesPerConfig.put(config, properties);
            }
            return properties;
        }

        HashMap<ConfigSpec, RootFilesProperties> getPropertiesPerConfigMap() {
            return propertiesPerConfig;
        }
    }

    static enum RootKeyType {
        FILE(null, false, "root files"), //
        FOLDER("folder", true, "root files with folder specification"), //
        PERMISSION("permissions", true, "root file permissions"), //
        LINKS("link", false, "root file links"); //

        final String keySegment;

        private boolean hasParameter;

        private final String message;

        RootKeyType(String keySegment, boolean hasParameter, String message) {
            this.keySegment = keySegment;
            this.hasParameter = hasParameter;
            this.message = message;
        }

        String getSyntaxErrorMessage(String[] keySegments) {
            return String.join(".", keySegments) + " is an invalid key for " + message;
        }
    }

    static final String ROOT_KEY_SEGMENT = "root";

    private static final String LITERAL_DOT_EXPRESSION = "\\.";

    File baseDir;

    Map<String, String> rootEntries;

    ParsingResult parsingResult = new ParsingResult();

    String[] keySegments;

    String[] valueSegments;

    private boolean useDefaultExcludes;

    public RootPropertiesParser(File baseDir, BuildProperties buildProperties) {
        this.baseDir = baseDir;
        this.rootEntries = buildProperties.getRootEntries();
        this.useDefaultExcludes = buildProperties.isRootFilesUseDefaultExcludes();
    }

    public HashMap<ConfigSpec, RootFilesProperties> getPermissionsAndLinksResult() {
        return parsingResult.getPropertiesPerConfigMap();
    }

    public void parse() {
        for (Entry<String, String> entry : rootEntries.entrySet()) {
            keySegments = splitKey(entry.getKey());
            valueSegments = splitAndTrimValue(entry.getValue());
            parseBuildPropertiesLine();
        }
        resolvePermissionWildcards();
    }

    private void resolvePermissionWildcards() {
        for (RootFilesProperties rootProperty : parsingResult.getPropertiesPerConfigMap().values()) {
            rootProperty.resolvePermissionWildcards(useDefaultExcludes);
        }
    }

    private static String[] splitKey(String string) {
        // retain empty segments at the end - silently ignoring them is not a good option
        return string.split(LITERAL_DOT_EXPRESSION, -1);
    }

    private static String[] splitAndTrimValue(String value) {
        String[] segments = value.split(",");
        for (int ix = 0; ix < segments.length; ix++) {
            segments[ix] = segments[ix].trim();
        }
        return segments;
    }

    private void parseBuildPropertiesLine() {
        if (segmentEquals(keySegments, 0, ROOT_KEY_SEGMENT)) {
            parseRootPropertiesLine();
        }
    }

    void parseRootPropertiesLine() {
        for (RootKeyType keyType : RootKeyType.values()) {
            int indexOfTypeSegment = findTypeInKey(keySegments, keyType);
            if (indexOfTypeSegment < 0) {
                // not a line of keyType
                continue;
            }
            String parameter = getParameterFromKey(keySegments, indexOfTypeSegment);
            ConfigSpec config = getConfigFromKey(keySegments, indexOfTypeSegment);
            verifyNumberOfParameters(keySegments, keyType, parameter);

            storeRootPropertyValue(keyType, parameter, parsingResult.getPropertiesForWriting(config));

            // line has been recognized
            return;
        }
        throw new IllegalArgumentException(String.join(".", keySegments) + " is an invalid root key");
    }

    void storeRootPropertyValue(RootKeyType keyType, String parameterInKey, RootFilesProperties target) {
        switch (keyType) {
        case FILE:
            RootFilePatternParser filePatternParser = new RootFilePatternParser(baseDir, target, useDefaultExcludes);
            filePatternParser.addFilesFromPatterns(valueSegments, "");
            break;
        case FOLDER:
            RootFilePatternParser filePatternParserWithinFolder = new RootFilePatternParser(baseDir, target, useDefaultExcludes);
            // parameterInKey is the destination subdirectory
            // (root.folder.<subdir> and root.<config>.folder.<subdir>)
            filePatternParserWithinFolder.addFilesFromPatterns(valueSegments, parameterInKey);
            break;
        case PERMISSION:
            target.addPermission(parameterInKey, valueSegments);
            break;
        case LINKS:
            target.addLinks(valueSegments);
            break;
        }
    }

    static int findTypeInKey(String[] keySegments, RootKeyType keyType) {
        for (int candidateIndex : new int[] { 1, 4 }) {
            if (segmentEqualsOrIsEndSegment(keySegments, candidateIndex, keyType.keySegment)) {
                return candidateIndex;
            }
        }
        return -1;
    }

    static ConfigSpec getConfigFromKey(String[] keySegments, int indexOfKeyType) {
        if (indexOfKeyType == 4) {
            return ConfigSpec.createFromOsWsArchArray(keySegments, 1);
        }
        return ConfigSpec.GLOBAL;
    }

    static String getParameterFromKey(String[] keySegments, int indexOfKeyType) {
        int indexOfLastSegment = keySegments.length - 1;
        int parameters = indexOfLastSegment - indexOfKeyType;
        if (parameters > 1) {
            String param = "";
            for (int i = 1; i <= parameters; i++) {
                param += keySegments[indexOfKeyType + i];
                if (i < parameters)
                    param += ".";
            }
            return param;
        } else if (parameters == 1)
            return keySegments[indexOfKeyType + 1];
        else
            return null;
    }

    static void verifyNumberOfParameters(String[] keySegments, RootKeyType keyType, String parameter) {
        boolean keyHasArgument = parameter != null;
        boolean typeExpectsParameter = keyType.hasParameter;
        if (typeExpectsParameter != keyHasArgument) {
            throw new IllegalArgumentException(keyType.getSyntaxErrorMessage(keySegments));
        }
    }
}
