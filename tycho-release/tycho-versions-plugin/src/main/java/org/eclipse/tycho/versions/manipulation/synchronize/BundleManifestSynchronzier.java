package org.eclipse.tycho.versions.manipulation.synchronize;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.versions.bundle.MutableBundleManifest;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.engine.VersionChangesDescriptor;
import org.eclipse.tycho.versions.manipulation.BundleManifestManipulator;

@Component(role = MetadataManipulator.class, hint = "bundle-manifest-synchronizer")
public class BundleManifestSynchronzier extends BundleManifestManipulator {

    @Override
    protected VersionChange findVersionChangeForProject(ProjectMetadata project,
            VersionChangesDescriptor versionChangeContext) {
        MutableBundleManifest mf = getBundleManifest(project);
        VersionChange versionChangeForProject = versionChangeContext
                .findVersionChangeByArtifactId(mf.getSymbolicName());
        return versionChangeForProject;
    }
}
