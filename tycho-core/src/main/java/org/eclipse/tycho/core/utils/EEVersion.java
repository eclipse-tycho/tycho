/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.utils;

import org.osgi.framework.Version;

public class EEVersion implements Comparable<EEVersion> {

    public enum EEType {

        // order is significant for comparison
        OSGI_MINIMUM("OSGi/Minimum"), CDC_FOUNDATION("CDC/Foundation"), JAVA_SE("JavaSE");

        private final String profileName;

        private EEType(String profileName) {
            this.profileName = profileName;
        }

        public static EEType fromName(String profileName) {
            for (EEType type : values()) {
                if (type.profileName.equals(profileName)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(profileName);
        }
    }

    private Version version;
    private EEType type;

    public EEVersion(Version version, EEType type) {
        this.version = version;
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(EEVersion other) {
        int result = type.compareTo(other.type);
        if (result != 0) {
            return result;
        }
        return version.compareTo(other.version);
    }

}
