/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.versionbump;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.eclipse.tycho.p2tools.copiedfromp2.QueryableArray;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * This mojo updates the dependencies in a manifest with the current used build versions.
 * <p>
 * It is recommended to use as the lower bound the dependency the code was compiled with to avoid
 * using newer code from dependencies, but managing that manually can be a daunting task.
 * </p>
 * <p>
 * This can be used to automate the task for example after dependencies are updated:
 * 
 * <pre>
 * mvn tycho-version-bump:update-manifest
 * </pre>
 * </p>
 */
@Mojo(name = "update-manifest", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class UpdateManifestMojo extends AbstractUpdateMojo {

    @Parameter(defaultValue = "${project.basedir}/" + JarFile.MANIFEST_NAME, property = "manifestFile")
    private File manifestFile;

    /**
     * If set add missing upper bounds for dependencies
     */
    @Parameter(defaultValue = "true", property = "upper")
    private boolean addMissingUpperBounds;

    /**
     * If set add missing lower bounds for dependencies
     */
    @Parameter(defaultValue = "true", property = "lower")
    private boolean addMissingLowerBounds;

    @Component
    private TychoProjectManager projectManager;

    @Override
    protected void doUpdate(File file) throws Exception {
        MutableBundleManifest manifest = MutableBundleManifest.read(file);
        DependencyArtifacts artifacts = projectManager.getDependencyArtifacts(getProject()).orElse(null);
        if (artifacts != null) {
            QueryableArray queryable = new QueryableArray(artifacts.getInstallableUnits());
            Map<String, String> updatedBundles = updateRequiredBundles(manifest, queryable);
            if (!updatedBundles.isEmpty()) {
                manifest.updateRequiredBundleVersions(updatedBundles);
                MutableBundleManifest.write(manifest, file);
            }
        }
    }

    private Map<String, String> updateRequiredBundles(MutableBundleManifest manifest, QueryableArray queryable) {
        Map<String, String> updatedBundles = new HashMap<>();
        for (Entry<String, String> requiredBundle : manifest.getRequiredBundleVersions().entrySet()) {
            String oldValue = requiredBundle.getValue();
            String bsn = requiredBundle.getKey();
            IInstallableUnit unit = queryable.query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(bsn)), null)
                    .stream().findFirst().orElse(null);
            if (unit != null) {
                Version latestVersion = Version.parseVersion(unit.getVersion().toString());
                String format = toVersionString(latestVersion);
                if (oldValue == null || oldValue.isBlank()) {
                    //no value at all
                    if (addMissingUpperBounds && addMissingLowerBounds) {
                        updatedBundles.put(bsn, String.format("[%s,%d)", toVersionString(latestVersion),
                                (latestVersion.getMajor() + 1)));
                    } else if (addMissingLowerBounds) {
                        updatedBundles.put(bsn, toVersionString(latestVersion));
                    }
                } else if (oldValue.startsWith("[") || oldValue.startsWith("(")) {
                    //a version range to handle
                    VersionRange range = VersionRange.valueOf(oldValue);
                    if (!versionEqualsIgnoreQualifier(range.getLeft(), latestVersion)) {
                        if (isStrict(range)) {
                            String newVersionRange = String.format("[%s,%s]", format, format);
                            getLog().info("Update strict version range of required bundle " + bsn + " from " + oldValue
                                    + " to " + newVersionRange);
                            updatedBundles.put(bsn, newVersionRange);
                        } else {
                            String newVersionRange = String.format("[%s,%s)", format, range.getRight().toString());
                            getLog().info("Update version range of required bundle " + bsn + " from " + oldValue
                                    + " to " + newVersionRange);
                            updatedBundles.put(bsn, newVersionRange);
                        }
                    }
                } else {
                    Version currentVersion = Version.parseVersion(oldValue);
                    //a plain version to handle
                    if (latestVersion.compareTo(currentVersion) > 0
                            && !versionEqualsIgnoreQualifier(currentVersion, latestVersion)) {
                        getLog().info(
                                "Update lower bound of required bundle " + bsn + " from " + oldValue + " to " + format);
                        updatedBundles.put(bsn, format);
                        oldValue = format;
                    }
                    if (addMissingUpperBounds) {
                        updatedBundles.put(bsn, String.format("[%s,%d)", oldValue, (latestVersion.getMajor() + 1)));
                    }
                }
            }
        }
        return updatedBundles;
    }

    private boolean isStrict(VersionRange range) {
        if (range.isExact()) {
            return true;
        }
        return false;
    }

    private String toVersionString(Version version) {
        return String.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro());
    }

    private boolean versionEqualsIgnoreQualifier(Version v1, Version v2) {
        return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getMicro() == v1.getMicro();
    }

    @Override
    protected File getFileToBeUpdated() throws MojoExecutionException, MojoFailureException {

        return manifestFile;
    }

}
