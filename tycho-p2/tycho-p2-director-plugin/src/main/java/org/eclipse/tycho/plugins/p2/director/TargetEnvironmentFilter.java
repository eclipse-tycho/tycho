/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import org.eclipse.tycho.core.facade.TargetEnvironment;

// TODO use this class whenever any of ws/os/arch may be omitted, and then make TargetEnvironment forcibly ws/os/arch complete
public class TargetEnvironmentFilter {

    private final String os;
    private final String ws;
    private final String arch;
    private final int filteredFieldCount;

    public static TargetEnvironmentFilter parseConfigurationElement(String value) {
        ConfigurationElementParserRun parser = new ConfigurationElementParserRun(value);
        parser.run();
        return new TargetEnvironmentFilter(parser.parsedOs, parser.parsedWs, parser.parsedArch, parser.explicitSegments);
    }

    private TargetEnvironmentFilter(String os, String ws, String arch, int filteredFieldCount) {
        this.os = os;
        this.ws = ws;
        this.arch = arch;
        this.filteredFieldCount = filteredFieldCount;
    }

    public boolean matches(TargetEnvironment env) {
        return (os == null || os.equals(env.getOs())) && (ws == null || ws.equals(env.getWs()))
                && (arch == null || arch.equals(env.getArch()));
    }

    public int filteredFieldCount() {
        return filteredFieldCount;
    }

    private static class ConfigurationElementParserRun {

        private String[] segments;
        private int lastExplicitSegment = 0;
        int explicitSegments = 0;

        String parsedOs;
        String parsedWs;
        String parsedArch;

        ConfigurationElementParserRun(String value) {
            this.segments = value.split("\\.");
        }

        void run() {
            if (segments.length > 3) {
                throw new IllegalArgumentException(
                        "Target environment configuration elements does not match format \"os.ws.arch\"");
            }

            parsedOs = parseSegmentIfPresent(0);
            parsedWs = parseSegmentIfPresent(1);
            parsedArch = parseSegmentIfPresent(2);

            if (lastExplicitSegment < segments.length - 1) {
                throw new IllegalArgumentException(
                        "Trailing \".any\" wildcards must be omitted in target environment configuration elements");
            }
        }

        private String parseSegmentIfPresent(int segmentIx) {
            if (segmentIx < segments.length) {
                return parseSegment(segmentIx);
            }
            return null;
        }

        private String parseSegment(int segmentIx) {
            String value = segments[segmentIx];
            if ("any".equals(value)) {
                return null;
            } else {
                lastExplicitSegment = segmentIx;
                explicitSegments++;
                return value;
            }
        }
    }
}
