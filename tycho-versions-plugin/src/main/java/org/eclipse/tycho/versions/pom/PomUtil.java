/*******************************************************************************
 * Copyright (c) 2024 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.pom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomUtil {

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    /**
     * Returns whether the string contains properties <code>${properties}</code>.
     */
    public static boolean containsProperties(String str) {
        return str != null && str.contains("${");
    }

    /**
     * Expands properties in the given string.
     * <p>
     * If a property is not found it is left unexpanded.
     *
     * @param str
     *            the input string
     * @param properties
     *            possible replacement properties
     * @return the expanded string
     */
    public static String expandProperties(String str, List<Property> properties) {
        if (containsProperties(str)) {
            StringBuilder resolvedVersionBuilder = new StringBuilder();
            Matcher m = PROPERTY_PATTERN.matcher(str.trim());
            while (m.find()) {
                String unexpandedProperty = m.group();
                String propertyName = m.group(1);
                m.appendReplacement(resolvedVersionBuilder,
                        Matcher.quoteReplacement(properties.stream().filter(p -> p.getName().equals(propertyName))
                                .map(p -> p.getValue()).findFirst().orElse(unexpandedProperty)));
            }
            m.appendTail(resolvedVersionBuilder);
            return resolvedVersionBuilder.toString();
        } else {
            return str;
        }
    }

    /**
     * Returns the list of property names that make up the given string.
     */
    public static List<String> getContainedPropertyNames(String str) {
        if (containsProperties(str)) {
            Matcher m = PROPERTY_PATTERN.matcher(str.trim());
            List<String> propertyNames = new ArrayList<>();
            while (m.find()) {
                propertyNames.add(m.group(1));
            }
            return Collections.unmodifiableList(propertyNames);
        }
        return List.of();
    }
}
