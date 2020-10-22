/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildversion;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A helper for discovering common timestamps in strings
 */
public class TimestampFinder {

    private static Map<SimpleDateFormat, Pattern> defaultPatterns() {
        Map<SimpleDateFormat, Pattern> result = new LinkedHashMap<>();
        result.put(utcFormat("yyyyMMddHHmm"), Pattern.compile("([0-9]{12})"));
        result.put(utcFormat("yyyyMMdd-HHmm"), Pattern.compile("([0-9]{8})-([0-9]{4})"));
        result.put(utcFormat("yyyyMMdd"), Pattern.compile("([0-9]{8})"));
        return result;
    }

    private static SimpleDateFormat utcFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private final Map<SimpleDateFormat, Pattern> datePatternsByRegularExpressions;

    public TimestampFinder() {
        datePatternsByRegularExpressions = defaultPatterns();
    }

    public Date findInString(String string) {
        for (Entry<SimpleDateFormat, Pattern> e : datePatternsByRegularExpressions.entrySet()) {
            Matcher matcher = e.getValue().matcher(string);
            if (matcher.find()) {
                String group = matcher.group();
                Date timestamp = parseTimestamp(group, e.getKey());
                if (timestamp != null)
                    return timestamp;
            }
        }
        return null;
    }

    private Date parseTimestamp(String timestampString, SimpleDateFormat format) {
        ParsePosition pos = new ParsePosition(0);
        Date timestamp = format.parse(timestampString, pos);
        if (timestamp != null && pos.getIndex() == timestampString.length()) {
            return timestamp;
        }
        return null;
    }
}
