/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
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
package org.eclipse.tycho.p2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CommandLineArguments {
    List<String> arguments = new ArrayList<>();

    public void add(String flag) {
        arguments.add(flag);
    }

    public void add(String parameterName, String parameterValue) {
        arguments.add(parameterName);
        arguments.add(parameterValue);
    }

    public void addNonNull(String parameterName, String parameterValue) {
        if (parameterValue != null) {
            arguments.add(parameterName);
            arguments.add(parameterValue);
        }
    }

    public void addUnlessEmpty(String parameterName, StringJoiner parameterValue) {
        if (parameterValue.length() > 0) {
            add(parameterName, parameterValue.toString());
        }
    }

    public void addNotEmpty(String parameterName, List<String> list, CharSequence seperator) {
        if (list.isEmpty()) {
            return;
        }
        add(parameterName, list.stream().collect(Collectors.joining(seperator)));
    }

    public void addNotEmpty(String parameterName, Map<String, String> propertyMap, CharSequence keyValueSeparator,
            CharSequence seperator) {
        if (propertyMap.isEmpty()) {
            return;
        }
        add(parameterName,
                propertyMap.entrySet().stream().map(entry -> entry.getKey() + keyValueSeparator + entry.getValue())
                        .collect(Collectors.joining(seperator)));
    }

    public void addFlagIfTrue(String flag, boolean value) {
        if (value) {
            add(flag);
        }
    }

    public void addNonNull(String parameterName, File file) {
        if (file != null) {
            add(parameterName, file.getAbsolutePath());
        }
    }

    public List<String> asList() {
        return new ArrayList<>(arguments);
    }

    public String[] toArray() {
        return arguments.toArray(String[]::new);
    }

    @Override
    public String toString() {
        return arguments.stream().collect(Collectors.joining(" "));
    }

}
