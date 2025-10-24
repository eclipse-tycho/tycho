/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Sebastien Arod - update version ranges
 *******************************************************************************/
package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.model.manifest.MutableBundleManifest;
import org.eclipse.tycho.versions.bundle.MutableBndFile;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.PackageVersionChange;
import org.eclipse.tycho.versions.engine.PomVersionChange;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.engine.Versions;
import org.osgi.framework.Constants;

@Named("bundle-manifest")
@Singleton
public class BundleManifestManipulator extends AbstractMetadataManipulator {

    @Override
    public boolean addMoreChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isBundle(project)) {
            Set<PackageVersionChange> changes = computeExportedPackageChanges(project, versionChangeContext);
            return versionChangeContext.addPackageVersionChanges(changes);
        }
        return false;
    }

    @Override
    public Collection<String> validateChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isBundle(project)) {
            VersionChange versionChangeForProject = findVersionChangeForProject(project, versionChangeContext);
            if (versionChangeForProject != null) {
                String error = Versions.validateOsgiVersion(versionChangeForProject.getNewVersion(),
                        getManifestFile(project));
                return error != null ? Collections.singleton(error) : null;
            }
        }
        return null;
    }

    @Override
    public void applyChanges(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        if (isBundle(project)) {
            updateBundleAndExportPackageVersions(project, versionChangeContext);

            updateFragmentHostVersion(project, versionChangeContext);

            updateRequireBundleVersions(project, versionChangeContext);

            updateImportPackageVersions(project, versionChangeContext);
        }
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        MutableBundleManifest mf = project.getMetadata(MutableBundleManifest.class);
        if (mf != null) {
            MutableBundleManifest.write(mf, getManifestFile(project));
        }
        MutableBndFile bnd = project.getMetadata(MutableBndFile.class);
        if (bnd != null) {
            bnd.write(getBndFile(project));
        }
    }

    private Set<PackageVersionChange> computeExportedPackageChanges(ProjectMetadata project,
            VersionChangesDescriptor versionChangeContext) {
        VersionChange versionChangeForProject = findVersionChangeForProject(project, versionChangeContext);
        if (versionChangeForProject == null) {
            return Collections.emptySet();
        }

        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            return Set.of();
        }
        MutableBundleManifest mf = bundleManifest.get();
        // ignore ".qualifier" literals in package versions
        String versionToReplace = Versions.toBaseVersion(versionChangeForProject.getVersion());
        String newVersion = Versions.toBaseVersion(versionChangeForProject.getNewVersion());

        Set<PackageVersionChange> packageVersionChanges = new HashSet<>();
        for (Entry<String, String> exportedPackageVersion : mf.getExportedPackagesVersion().entrySet()) {
            String packageName = exportedPackageVersion.getKey();
            String packageVersion = exportedPackageVersion.getValue();
            if (packageVersion != null && packageVersion.equals(versionToReplace)) {
                packageVersionChanges
                        .add(new PackageVersionChange(mf.getSymbolicName(), packageName, packageVersion, newVersion));
            }
        }
        return packageVersionChanges;
    }

    private VersionChange findVersionChangeForProject(ProjectMetadata project,
            VersionChangesDescriptor versionChangeContext) {
        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            Optional<MutableBndFile> bndFile = getBundleBndFile(project);
            if (bndFile.isPresent()) {
                MutableBndFile bnd = bndFile.get();
                String bsn = bnd.getValue(Constants.BUNDLE_SYMBOLICNAME);
                String vrs = bnd.getValue(Constants.BUNDLE_VERSION);
                VersionChange versionChangeForProject = versionChangeContext.findVersionChangeByArtifactId(bsn);
                if (versionChangeForProject != null && versionChangeForProject.getVersion().equals(vrs)) {
                    return versionChangeForProject;
                }
            }
            return null;
        }
        MutableBundleManifest mf = bundleManifest.get();
        VersionChange versionChangeForProject = versionChangeContext
                .findVersionChangeByArtifactId(mf.getSymbolicName());
        if (versionChangeForProject != null && versionChangeForProject.getVersion().equals(mf.getVersion())) {
            return versionChangeForProject;
        } else {
            return null;
        }
    }

    private void updateBundleAndExportPackageVersions(ProjectMetadata project,
            VersionChangesDescriptor versionChangeContext) {
        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            Optional<MutableBndFile> bndFile = getBundleBndFile(project);
            if (bndFile.isPresent()) {
                MutableBndFile bnd = bndFile.get();
                VersionChange versionChangeForProject = findVersionChangeForProject(project, versionChangeContext);
                if (versionChangeForProject != null) {
                    logger.info("  " + TychoConstants.PDE_BND + "//Bundle-Version: "
                            + versionChangeForProject.getVersion() + " => " + versionChangeForProject.getNewVersion());
                    bnd.setValue(Constants.BUNDLE_VERSION, versionChangeForProject.getNewVersion());
                }
            }
            return;
        }
        MutableBundleManifest mf = bundleManifest.get();
        VersionChange versionChangeForProject = findVersionChangeForProject(project, versionChangeContext);
        if (versionChangeForProject != null) {
            logger.info("  META-INF/MANIFEST.MF//Bundle-Version: " + versionChangeForProject.getVersion() + " => "
                    + versionChangeForProject.getNewVersion());

            mf.setVersion(versionChangeForProject.getNewVersion());

            Map<String, String> exportPackagesNewVersion = new HashMap<>();
            for (PackageVersionChange packageVersionChange : versionChangeContext.getPackageVersionChanges()) {
                if (packageVersionChange.getBundleSymbolicName().equals(mf.getSymbolicName())) {
                    logger.info("  META-INF/MANIFEST.MF//Export-Package//" + packageVersionChange.getPackageName()
                            + ";version: " + packageVersionChange.getVersion() + " => "
                            + packageVersionChange.getNewVersion());
                    exportPackagesNewVersion.put(packageVersionChange.getPackageName(),
                            packageVersionChange.getNewVersion());
                }
            }
            mf.updateExportedPackageVersions(exportPackagesNewVersion);
        }
    }

    private void updateFragmentHostVersion(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            return;
        }
        MutableBundleManifest mf = bundleManifest.get();
        if (mf.isFragment()) {
            VersionChange versionChange = versionChangeContext
                    .findVersionChangeByArtifactId(mf.getFragmentHostSymbolicName());
            if (versionChange != null) {
                String newVersionRange = versionChangeContext.getVersionRangeUpdateStrategy().computeNewVersionRange(
                        mf.getFragmentHostVersion(), versionChange.getVersion(), versionChange.getNewVersion());
                logger.info("  META-INF/MANIFEST.MF//Fragment-Host//" + mf.getFragmentHostSymbolicName()
                        + ";bundle-version: " + newVersionRange + " => " + newVersionRange);

                mf.setFragmentHostVersion(Versions.toBaseVersion(newVersionRange));
            }
        }
    }

    private void updateRequireBundleVersions(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            return;
        }
        MutableBundleManifest mf = bundleManifest.get();
        Map<String, String> requiredBundleVersions = mf.getRequiredBundleVersions();
        Map<String, String> versionsToUpdate = new HashMap<>();
        for (PomVersionChange versionChange : versionChangeContext.getVersionChanges()) {
            String bundleSymbolicName = versionChange.getArtifactId();
            if (requiredBundleVersions.containsKey(bundleSymbolicName)) {
                String originalVersionRange = requiredBundleVersions.get(bundleSymbolicName);
                versionsToUpdate.put(bundleSymbolicName,
                        versionChangeContext.getVersionRangeUpdateStrategy().computeNewVersionRange(
                                originalVersionRange, versionChange.getVersion(), versionChange.getNewVersion()));
            }
        }
        mf.updateRequiredBundleVersions(versionsToUpdate);
    }

    private void updateImportPackageVersions(ProjectMetadata project, VersionChangesDescriptor versionChangeContext) {
        Optional<MutableBundleManifest> bundleManifest = getBundleManifest(project);
        if (bundleManifest.isEmpty()) {
            return;
        }
        MutableBundleManifest mf = bundleManifest.get();
        Map<String, String> importedPackageNewVersions = new HashMap<>();
        for (Entry<String, String> importPackageVersions : mf.getImportPackagesVersions().entrySet()) {
            String packageName = importPackageVersions.getKey();
            String importVersionRange = importPackageVersions.getValue();
            if (importVersionRange != null) {
                PackageVersionChange versionChange = versionChangeContext.findPackageVersionChange(packageName);
                if (versionChange != null) {
                    String newVersionRange = versionChangeContext.getVersionRangeUpdateStrategy()
                            .computeNewVersionRange(importVersionRange, versionChange.getVersion(),
                                    versionChange.getNewVersion());
                    logger.info("  META-INF/MANIFEST.MF//Import-Package//" + packageName + ";version: "
                            + importVersionRange + " => " + newVersionRange);
                    importedPackageNewVersions.put(packageName, newVersionRange);
                }
            }
        }
        mf.updateImportedPackageVersions(importedPackageNewVersions);
    }

    private Optional<MutableBundleManifest> getBundleManifest(ProjectMetadata project) {
        MutableBundleManifest mf = project.getMetadata(MutableBundleManifest.class);
        if (mf == null) {
            File file = getManifestFile(project);
            if (file.isFile()) {
                try {
                    mf = MutableBundleManifest.read(file);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not parse bundle manifest " + file, e);
                }
                project.putMetadata(mf);
                return Optional.of(mf);
            }
        }
        return Optional.ofNullable(mf);
    }

    private Optional<MutableBndFile> getBundleBndFile(ProjectMetadata project) {
        MutableBndFile bnd = project.getMetadata(MutableBndFile.class);
        if (bnd == null) {
            File file = getBndFile(project);
            if (file.isFile()) {
                try {
                    bnd = MutableBndFile.read(file);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not parse bundle manifest " + file, e);
                }
                project.putMetadata(bnd);
                return Optional.of(bnd);
            }
        }
        return Optional.ofNullable(bnd);
    }

    private File getManifestFile(ProjectMetadata project) {
        return new File(project.getBasedir(), "META-INF/MANIFEST.MF");
    }

    private File getBndFile(ProjectMetadata project) {
        return new File(project.getBasedir(), TychoConstants.PDE_BND);
    }

}
