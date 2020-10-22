/*******************************************************************************
 * Copyright (c) 2015 VDS Rail and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Enrico De Fent - initial API and implementation (see bug 459214)
 *******************************************************************************/
package org.eclipse.tycho.extras.docbundle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An utility class for filtering package names.
 */
public class PackageNameMatcher {

    /**
     * Compiles the given list of package name specification strings into a matching object.
     * 
     * <p>
     * If the list is empty, the resulting object will never match any package name, which means
     * that {@link #matches(String)} will always return <code>false</code>.
     * </p>
     * 
     * @param specs
     *            The list of package name specifications. For details on specification syntax see
     *            {@link #compile(String)}.
     * @return A new matching object.
     * @throws IllegalArgumentException
     *             Thrown if the given argument is <code>null</code> or if any of the strings in the
     *             list is an invalid package name specification.
     */
    public static PackageNameMatcher compile(final List<String> specs) {
        if (specs == null) {
            throw new IllegalArgumentException("null package name specifications");
        }

        final List<Pattern> list = new ArrayList<>();

        for (String part : specs) {
            list.add(buildPattern(part));
        }

        return new PackageNameMatcher(list);
    }

    private static Pattern buildPattern(final String spec) {
        if (spec.isEmpty()) {
            throw new IllegalArgumentException("empty package name");
        }

        final StringBuilder regex = new StringBuilder();

        for (int idx = 0; idx < spec.length(); idx++) {
            char ch = spec.charAt(idx);
            if (ch == '*') {
                regex.append(".*");

            } else if (ch == '.') {
                regex.append("\\.");

            } else if (idx == 0 && !Character.isJavaIdentifierStart(ch)) {
                throw new IllegalArgumentException("invalid package name: " + spec);

            } else if (!Character.isJavaIdentifierPart(ch)) {
                throw new IllegalArgumentException("invalid package name: " + spec);

            } else {
                regex.append(ch);
            }
        }

        try {
            return Pattern.compile(regex.toString());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("invalid package specification: " + spec, e);
        }
    }

    private final List<Pattern> patterns;

    private final String description;

    /**
     * Constructs a new object which matches against the given patterns.
     * 
     * @param patterns
     */
    private PackageNameMatcher(List<Pattern> patterns) {
        this.patterns = patterns;
        this.description = buildDescription();
    }

    private String buildDescription() {
        final StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < patterns.size(); idx++) {
            sb.append('"').append(patterns.get(idx).pattern()).append('"');
            if (idx < patterns.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if the given package name matches against any of the patterns in this matcher.
     *
     * @return Always <code>false</code> if the given package name is <code>null</code> or an empty
     *         string.
     */
    public boolean matches(String packageName) {
        if (packageName.isEmpty()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(packageName).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), description);
    }

}
