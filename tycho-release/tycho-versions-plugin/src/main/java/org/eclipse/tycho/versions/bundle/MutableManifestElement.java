/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Arod - Initial implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

/**
 * This class is similar to {@link ManifestElement} but supports changing value for attributes with
 * a single value.
 * 
 */
public class MutableManifestElement {

    private static final String ATTRIBUTE_NAME_VALUE_SEP = "=";
    private static final String DIRECTIVE_NAME_VALUE_SEP = ":=";

    /**
     * See extended definition in osgi core spec at paragraph 1.3.2 General Syntax Definitions
     */
    private static final Pattern OSGI_EXTENDED_PATTERN = Pattern.compile("([a-zA-Z0-9_-]|\\+)+");

    private final String value;

    // Use LinkedHashMap to preserve original order
    private final LinkedHashMap<String, List<String>> directives;

    // Use LinkedHashMap to preserve original order
    private final LinkedHashMap<String, List<String>> attributes;

    public MutableManifestElement(String value, Map<String, List<String>> attributes,
            Map<String, List<String>> directives) {
        this.value = value;
        this.attributes = defensiveCopy(attributes);
        this.directives = defensiveCopy(directives);
    }

    private LinkedHashMap<String, List<String>> defensiveCopy(Map<String, List<String>> attributes) {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
        for (Entry<String, List<String>> entry : attributes.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return copy;
    }

    public static MutableManifestElement fromManifestElement(ManifestElement manifestElement) {
        LinkedHashMap<String, List<String>> attributes = new LinkedHashMap<>();
        LinkedHashMap<String, List<String>> directives = new LinkedHashMap<>();

        Enumeration<String> attrKeys = manifestElement.getKeys();
        if (attrKeys != null) {
            while (attrKeys.hasMoreElements()) {
                String attrKey = attrKeys.nextElement();
                attributes.put(attrKey, Arrays.asList(manifestElement.getAttributes(attrKey)));
            }
        }
        Enumeration<String> directiveKeys = manifestElement.getDirectiveKeys();
        if (directiveKeys != null) {
            while (directiveKeys.hasMoreElements()) {
                String directiveName = directiveKeys.nextElement();
                directives.put(directiveName, Arrays.asList(manifestElement.getDirectives(directiveName)));
            }
        }
        return new MutableManifestElement(manifestElement.getValue(), attributes, directives);
    }

    public static List<MutableManifestElement> parseHeader(String name, String value) throws BundleException {
        ManifestElement[] manifestElements = ManifestElement.parseHeader(name, value);
        if (manifestElements == null) {
            return null;
        }
        List<MutableManifestElement> mutableManifestElements = new ArrayList<>();
        for (ManifestElement manifestElement : manifestElements) {
            mutableManifestElements.add(MutableManifestElement.fromManifestElement(manifestElement));
        }
        return mutableManifestElements;
    }

    /**
     * 
     * @return
     * @see ManifestElement#getValue()
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the value for the specified attribute. If previous values existed they are replaced.
     * 
     * @param key
     * @param value
     */
    public void setAttribute(String key, String value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, Collections.singletonList(value));
        }
    }

    /**
     * Returns the value for the specified attribute or <code>null</code> if it does not exist. If
     * the attribute has multiple values specified then the last value specified is returned.
     * 
     * @param key
     * @return
     * @see ManifestElement#getAttribute(String)
     */
    public String getAttribute(String key) {
        List<String> values = attributes.get(key);
        if (values != null && !values.isEmpty()) {
            return values.get(values.size() - 1);
        }
        return null;
    }

    @Override
    public String toString() {
        if (directives.isEmpty() && attributes.isEmpty()) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value);
        for (Entry<String, List<String>> attribute : attributes.entrySet()) {
            appendParameter(builder, attribute.getKey(), attribute.getValue(), ATTRIBUTE_NAME_VALUE_SEP);
        }
        for (Entry<String, List<String>> directive : directives.entrySet()) {
            appendParameter(builder, directive.getKey(), directive.getValue(), DIRECTIVE_NAME_VALUE_SEP);
        }
        return builder.toString();
    }

    private void appendParameter(StringBuilder builder, String name, List<String> values, String sep) {
        for (String value : values) {
            builder.append(';');
            builder.append(name);
            builder.append(sep);

            boolean needsQuote = !OSGI_EXTENDED_PATTERN.matcher(value).matches();
            if (needsQuote) {
                builder.append('\"').append(value).append('\"');
            } else {
                builder.append(value);
            }

        }
    }

}
