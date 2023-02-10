/*******************************************************************************
 * Copyright (c) 2011, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/

package org.eclipse.tycho.core.ee;

import java.util.Objects;

import org.osgi.framework.Version;

public class EEVersion implements Comparable<EEVersion> {

    public enum EEType {

        // order is significant for comparison
        OSGI_MINIMUM("OSGi/Minimum"), CDC_FOUNDATION("CDC/Foundation"), JRE("JRE"), JAVA_SE("JavaSE"), JAVA_SE_COMPACT1(
                "JavaSE/compact1"), JAVA_SE_COMPACT2("JavaSE/compact2"), JAVA_SE_COMPACT3("JavaSE/compact3");

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
            return null;
        }
    }

    private static final Version JAVA8 = Version.parseVersion("1.8");
    private final Version version;
    private final EEType type;

    public EEVersion(Version version, EEType type) {
        Objects.requireNonNull(version);
        Objects.requireNonNull(type);
        this.version = version;
        this.type = type;
    }

    @Override
    public int compareTo(EEVersion other) {
        // JavaSE/compact{1..3} > JavaSE-N except when N >= 1.8 
        if (type.equals(EEType.JAVA_SE) && version.compareTo(JAVA8) >= 0
                && other.type.profileName.contains("JavaSE/compact")) {
            return 1;
        } else if (other.type.equals(EEType.JAVA_SE) && other.version.compareTo(JAVA8) >= 0
                && type.profileName.contains("JavaSE/compact")) {
            return -1;
        }

        int result = type.compareTo(other.type);
        if (result != 0) {
            return result;
        }
        return version.compareTo(other.version);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || //
                (other instanceof EEVersion o && //
                        this.version.equals(o.version) && //
                        this.type.equals(o.type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version);
    }

}
