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

import org.codehaus.plexus.configuration.PlexusConfiguration;

public class EnvironmentSpecificConfiguration {

    public enum Parameter {
        PROFILE_NAME, ARCHIVE_FORMAT

    }

    private String profileName;
    private String archiveFormat;

    public static EnvironmentSpecificConfiguration parse(PlexusConfiguration environmentConfiguration) {
        return new EnvironmentSpecificConfiguration(environmentConfiguration);
    }

    private EnvironmentSpecificConfiguration(PlexusConfiguration environmentConfiguration) {
        this.profileName = getStringValueOfChild(environmentConfiguration, "profileName");
        this.archiveFormat = getStringValueOfChild(environmentConfiguration, "archiveFormat");
    }

    public EnvironmentSpecificConfiguration(String profileName, String archiveFormat) {
        this.profileName = profileName;
        this.archiveFormat = archiveFormat;
    }

    public String getValue(Parameter parameter) {
        switch (parameter) {
        case PROFILE_NAME:
            return profileName;
        case ARCHIVE_FORMAT:
            return archiveFormat;
        default:
            throw new IllegalArgumentException();
        }
    }

    static String getStringValueOfChild(PlexusConfiguration configuration, String childName) {
        return getStringValue(configuration.getChild(childName));
    }

    static String getStringValue(PlexusConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        String value = configuration.getValue();
        if (value == null) {
            return null;
        }
        value = value.trim();
        if ("".equals(value)) {
            return null;
        }
        return value;
    }

}
