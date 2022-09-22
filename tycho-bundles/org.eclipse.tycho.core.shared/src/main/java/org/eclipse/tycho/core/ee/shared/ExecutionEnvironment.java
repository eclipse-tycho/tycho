/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.ee.shared;

import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

public interface ExecutionEnvironment {

    public static final class SystemPackageEntry {
        public final String packageName;

        /**
         * May be null
         */
        public final String version;

        public SystemPackageEntry(String packageName, String version) {
            this.packageName = packageName;
            this.version = version;
        }

        public String toPackageSpecifier() {
            if (version != null) {
                return packageName + ";version=\"" + version + "\"";
            } else {
                return packageName;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, version);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof SystemPackageEntry other && //
                    Objects.equals(this.packageName, other.packageName) && //
                    Objects.equals(this.version, other.version);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '[' + packageName + '/' + version + ']';
        }
    }

    String getProfileName();

    /**
     * Returns the list of packages (without versions) provided by the execution environment.
     */
    Collection<SystemPackageEntry> getSystemPackages();

    Properties getProfileProperties();

    /**
     * Returns a reasonable compiler source level default for this execution environment.
     * 
     * @return a compiler source level matching the execution environment, or <code>null</code> if
     *         unknown.
     */
    String getCompilerSourceLevelDefault();

    /**
     * Returns a reasonable compiler target level default for this execution environment.
     * 
     * @return a compiler target level matching the execution environment, or <code>null</code> if
     *         unknown.
     */
    String getCompilerTargetLevelDefault();

    /**
     * Returns <code>false</code> if classes compiled with the given compiler target level can
     * certainly not be executed on this execution environment. Used to detect inconsistent
     * configuration.
     */
    boolean isCompatibleCompilerTargetLevel(String targetLevel);

}
