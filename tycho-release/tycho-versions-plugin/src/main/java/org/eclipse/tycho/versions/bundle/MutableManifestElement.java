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
import org.osgi.framework.Constants;

/**
 * This class is similar to {@link ManifestElement} but supports changing value for attributes with
 * a single value.
 * 
 */
public class MutableManifestElement {

    private enum ParameterType {
        DIRECTIVE(":="), ATTRIBUTE("=");

        public final String separator;

        ParameterType(String separator) {
            this.separator = separator;
        }

    }

    /**
     * See extended definition in osgi core spec at paragraph 1.3.2 General Syntax Definitions
     */
    private static final Pattern OSGI_EXTENDED_PATTERN = Pattern.compile("([a-zA-Z0-9_-]|\\+)+");

    private static final String DIRECTIVE_INDENT = "  ";
    private static final String VALUE_COMPONENT_INDENT = "   ";

    private static final int NO_WRAP = Integer.MAX_VALUE;

    private static final String XFRIENDS_DIRECTIVE = "x-friends";

    private static final int DEFAULT_WRAP_FOR_LONG_DIRECTIVES = 3;

    private final String value;

    // Use LinkedHashMap to preserve original order
    private final LinkedHashMap<String, List<String>> directives;

    // Use LinkedHashMap to preserve original order
    private final LinkedHashMap<String, List<String>> attributes;

    public MutableManifestElement(String value, Map<String, String> attributes, Map<String, String> directives) {
        this.value = value;
        this.attributes = toMapOfSingletonLists(attributes);
        this.directives = toMapOfSingletonLists(directives);
    }

    private LinkedHashMap<String, List<String>> toMapOfSingletonLists(Map<String, String> params) {
        LinkedHashMap<String, List<String>> withSingletonLists = new LinkedHashMap<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            withSingletonLists.put(param.getKey(), Collections.singletonList(param.getValue()));
        }
        return withSingletonLists;
    }

    private MutableManifestElement(ManifestElement manifestElement) {
        // FIXME should we keep support for duplicate directives/attributes this does not seem to be valid OSGI and adds useless complexity.        
        this.value = manifestElement.getValue();
        this.attributes = new LinkedHashMap<>();
        this.directives = new LinkedHashMap<>();

        Enumeration<String> attrKeys = manifestElement.getKeys();
        if (attrKeys != null) {
            while (attrKeys.hasMoreElements()) {
                String attrKey = attrKeys.nextElement();
                this.attributes.put(attrKey, Arrays.asList(manifestElement.getAttributes(attrKey)));
            }
        }
        Enumeration<String> directiveKeys = manifestElement.getDirectiveKeys();
        if (directiveKeys != null) {
            while (directiveKeys.hasMoreElements()) {
                String directiveName = directiveKeys.nextElement();
                this.directives.put(directiveName, Arrays.asList(manifestElement.getDirectives(directiveName)));
            }
        }
    }

    public static List<MutableManifestElement> parseHeader(String name, String value) throws BundleException {
        ManifestElement[] manifestElements = ManifestElement.parseHeader(name, value);
        if (manifestElements == null) {
            return null;
        }
        List<MutableManifestElement> mutableManifestElements = new ArrayList<>();
        for (ManifestElement manifestElement : manifestElements) {
            mutableManifestElements.add(new MutableManifestElement(manifestElement));
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
     * @return returns true if the manifest element changed due to this call. false if the value was
     *         already the same.
     */
    public boolean setAttribute(String key, String value) {
        if (value == null) {
            if (attributes.containsKey(key)) {
                attributes.remove(key);
                return true;
            } else {
                return false;
            }
        } else {
            List<String> newValue = Collections.singletonList(value);
            if (!newValue.equals(attributes.get(key))) {
                attributes.put(key, Collections.singletonList(value));
                return true;
            } else {
                return false;
            }
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
        return write();
    }

    public String write() {
        if (directives.isEmpty() && attributes.isEmpty()) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value);

        // Write attributes
        for (Entry<String, List<String>> attribute : attributes.entrySet()) {
            for (String attributeValue : attribute.getValue()) {
                builder.append(writeParameter(attribute.getKey(), attributeValue, ParameterType.ATTRIBUTE, NO_WRAP));
            }
        }

        // Write directives
        // Follow the formatting of PDE for special directives "uses" and "x-friends" 
        // see org.eclipse.pde.internal.core.text.bundle.ExportPackageObject.appendValuesToBuffer(StringBuffer, TreeMap<String, Serializable>)
        List<String> uses = directives.get(Constants.USES_DIRECTIVE);
        List<String> xfriends = directives.get(XFRIENDS_DIRECTIVE);

        for (Entry<String, List<String>> directive : directives.entrySet()) {
            String directiveName = directive.getKey();
            if (!directiveName.equals(Constants.USES_DIRECTIVE) && !directiveName.equals(XFRIENDS_DIRECTIVE)) {
                for (String directiveValue : directive.getValue()) {
                    builder.append(writeParameter(directiveName, directiveValue, ParameterType.DIRECTIVE, NO_WRAP));
                }
            }
        }

        int longDirectiveLimit = DEFAULT_WRAP_FOR_LONG_DIRECTIVES;
        if (uses != null && xfriends != null) {
            longDirectiveLimit = 1;
        }

        if (xfriends != null) {
            for (String directiveValue : xfriends) {
                builder.append(writeParameter(XFRIENDS_DIRECTIVE, directiveValue, ParameterType.DIRECTIVE,
                        longDirectiveLimit));
            }
        }
        if (uses != null) {
            for (String directiveValue : uses) {
                builder.append(writeParameter(Constants.USES_DIRECTIVE, directiveValue, ParameterType.DIRECTIVE,
                        longDirectiveLimit));
            }
        }

        return builder.toString();
    }

    private static String writeParameter(String paramName, String parameterValue, ParameterType paramType,
            int valueComponentLimit) {
        String[] valueComponents = ManifestElement.getArrayFromList(parameterValue, ",");
        boolean breakLines = valueComponents.length >= valueComponentLimit;
        StringBuilder builder = new StringBuilder();
        builder.append(";");
        if (breakLines) {
            builder.append("\n" + DIRECTIVE_INDENT);
        }
        builder.append(paramName);
        builder.append(paramType.separator);
        if (paramValueNeedsQuotes(parameterValue)) {
            builder.append('\"');
            for (int i = 0; i < valueComponents.length; i++) {
                builder.append(valueComponents[i]);
                if (i != valueComponents.length - 1) {
                    builder.append(",");
                    if (breakLines) {
                        builder.append("\n" + VALUE_COMPONENT_INDENT);
                    }
                }
            }
            builder.append('\"');
        } else {
            builder.append(parameterValue);
        }
        return builder.toString();
    }

    private static boolean paramValueNeedsQuotes(String value) {
        return !OSGI_EXTENDED_PATTERN.matcher(value).matches();
    }

}
