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

import static org.eclipse.tycho.plugins.p2.director.RawConfigurationParserHelper.getStringValueOfChild;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class EnvironmentSpecificConfiguration {

    public enum Parameter {
        PROFILE_NAME, ARCHIVE_FORMAT

    }

    private String profileName;
    private String archiveFormat;

    public static EnvironmentSpecificConfiguration parse(Xpp3Dom entryDom) {
        return new EnvironmentSpecificConfiguration(entryDom);
    }

    private EnvironmentSpecificConfiguration(Xpp3Dom entryDom) {
        this.profileName = getStringValueOfChild(entryDom, "profileName");
        this.archiveFormat = getStringValueOfChild(entryDom, "archiveFormat");
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

}
