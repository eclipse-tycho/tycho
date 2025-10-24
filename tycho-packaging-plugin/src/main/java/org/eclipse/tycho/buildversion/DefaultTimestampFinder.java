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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.osgi.framework.Version;

/**
 * A helper for discovering common timestamps in strings
 */
@Named
@Singleton
public class DefaultTimestampFinder implements TimestampFinder {

	@Inject
	Logger logger;


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

    public DefaultTimestampFinder() {
        datePatternsByRegularExpressions = defaultPatterns();
    }

	@Override
	public Date findByDescriptor(ArtifactDescriptor artifact, SimpleDateFormat format) {
		ReactorProject otherProject = artifact.getMavenProject();
		String otherVersion = (otherProject != null) ? otherProject.getExpandedVersion()
				: artifact.getKey().getVersion();
		Version v = Version.parseVersion(otherVersion);
		String otherQualifier = v.getQualifier();
		if (otherQualifier != null) {
			Date qualifier = parseQualifier(otherQualifier, format);
			if (qualifier != null && logger.isDebugEnabled()) {
				logger.debug("Found '" + format.format(qualifier) + "' from qualifier '" + otherQualifier
						+ "' for artifact " + artifact);
			}
			return qualifier;
		} else {
			logger.debug("Could not parse qualifier timestamp " + otherQualifier);
		}
		return null;
	}

	private Date parseQualifier(String qualifier, SimpleDateFormat format) {
		ParsePosition pos = new ParsePosition(0);
		Date timestamp = format.parse(qualifier, pos);
		if (timestamp != null && pos.getIndex() == qualifier.length()) {
			return timestamp;
		}
		return discoverTimestamp(qualifier);
	}

	private Date discoverTimestamp(String qualifier) {
		return findInString(qualifier);
	}

	@Override
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
