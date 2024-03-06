/*******************************************************************************
 * Copyright (c) 2012, 2016 SAP SE and others.
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
package org.eclipse.tycho.p2.tools.director.shared;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.TargetEnvironment;

/**
 * A runtime environment of the p2 director application.
 */
public interface DirectorRuntime {

    /**
     * A command for a p2 director application.
     */
    public interface Command {

        void addMetadataSources(Iterable<URI> metadataRepositories);

        void addArtifactSources(Iterable<URI> artifactRepositories);

        void addUnitToInstall(String id);

        void addUnitToInstall(DependencySeed seed);

        void setProfileName(String profileName);

        void setEnvironment(TargetEnvironment env);

        void setInstallFeatures(boolean installFeatures);

        void setDestination(File path);

        void setVerifyOnly(boolean verifyOnly);

        void setBundlePool(File path);

        void execute() throws DirectorCommandException;

        void setProfileProperties(Map<String, String> profileProperties);

        /**
         * @return a copy of the effective profile properties
         */
        Map<String, String> getProfileProperties();

        void setPhaseSet(IPhaseSet phaseSet);

        void setInstallSources(boolean installSources);

        void setEEUnits(Collection<IInstallableUnit> eeUnits);
    }

    /**
     * Returns a new {@link Command} instance that can be used to execute a command with this
     * director runtime.
     */
    public Command newInstallCommand(String name);

    /**
     * Computes the destination of a director install based on a target environment
     * <p>
     * Currently, this implements special handling for macOS and behaves as follows:
     * <ul>
     * <li>If <code>baseLocation</code> already conforms to the full app bundle layout
     * (<code>/path/to/Foo.app/Contents/Eclipse</code>), <code>baseLocation</code> is returned
     * as-is.
     * <li>If <code>baseLocation</code> points to the root of an app bundle
     * (<code>/path/to/Foo.app</code>), <code>Contents/Eclipse</code> is appended and the path
     * <code>/path/to/Foo.app/Contents/Eclipse</code> is returned.
     * <li>Otherwise, i.e. if no app bundle path is given (<code>/path/to/work</code>), a valid app
     * bundle path is appended, and the path <code>/path/to/work/Eclipse.app/Contents/Eclipse</code>
     * is returned.
     * </ul>
     *
     * @param baseLocation
     *            the base location
     * @param env
     *            the target environment
     * @return the adjusted location to conform to layouts required by the target environment
     */
    public static File getDestination(File baseLocation, TargetEnvironment env) {
        if (PlatformPropertiesUtils.OS_MACOSX.equals(env.getOs())) {
            if (hasRequiredMacLayout(baseLocation)) {
                return baseLocation;
            } else if (isMacOsAppBundleRoot(baseLocation)) {
                return new File(baseLocation, "Contents/Eclipse/");
            } else {
                return new File(baseLocation, "Eclipse.app/Contents/Eclipse/");
            }
        }
        return baseLocation;
    }

    private static boolean hasRequiredMacLayout(File folder) {
        //TODO if we do not have this exact layout then director fails with:
        //The framework persistent data location (/work/MacOS/configuration) is not the same as the framework configuration location /work/Contents/Eclipse/configuration)
        //maybe we can simply configure the "persistent data location" to point to the expected one?
        //or the "configuration location" must be configured and look like /work/Contents/<work>/configuration ?
        //the actual values seem even depend on if this is an empty folder where one installs or an existing one
        //e.g. if one installs multiple env Equinox finds the launcher and then set the location different...
        if ("Eclipse".equals(folder.getName())) {
            File folder2 = folder.getParentFile();
            if (folder2 != null && "Contents".equals(folder2.getName())) {
                File parent = folder2.getParentFile();
                return parent != null && isMacOsAppBundleRoot(parent);
            }
        }
        return false;
    }

    private static boolean isMacOsAppBundleRoot(File folder) {
        return folder.getName().endsWith(".app");
    }

}
