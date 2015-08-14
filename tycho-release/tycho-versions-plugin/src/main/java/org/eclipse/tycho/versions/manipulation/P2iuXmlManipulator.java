package org.eclipse.tycho.versions.manipulation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.model.IU;
import org.eclipse.tycho.versions.engine.MetadataManipulator;
import org.eclipse.tycho.versions.engine.ProjectMetadata;
import org.eclipse.tycho.versions.engine.VersionChange;
import org.eclipse.tycho.versions.pom.MutablePomFile;

@Component(role = MetadataManipulator.class, hint = "p2-installable-unit")
public class P2iuXmlManipulator extends AbstractMetadataManipulator {

    @Override
    public void applyChange(ProjectMetadata project, VersionChange change, Set<VersionChange> allChanges) {
        if (!isIu(project)) {
            return;
        }
        IU iu = getIU(project);
        if (change.getVersion().equals(iu.getVersion()) && iu.getId().equals(change.getArtifactId())) {
            logger.info("  p2iu.xml//unit/@version: " + change.getVersion() + " => " + change.getNewVersion());
            iu.setVersion(change.getNewVersion());
            // version changed, so put the iu into the metadata so that the writeMetadata method can pick it up
            project.putMetadata(iu);
        }
    }

    @Override
    public void writeMetadata(ProjectMetadata project) throws IOException {
        if (!isIu(project)) {
            return;
        }
        IU iu = project.getMetadata(IU.class);
        if (iu != null) {
            IU.write(iu, new File(project.getBasedir(), IU.SOURCE_FILE_NAME));
        }
    }

    @Override
    public Collection<String> validateChange(ProjectMetadata project, VersionChange change) {
        return null;
    }

    private IU getIU(ProjectMetadata project) {
        IU iu = project.getMetadata(IU.class);
        if (iu == null) {
            iu = IU.loadIU(project.getBasedir());
        }
        return iu;
    }

    private boolean isIu(ProjectMetadata project) {
        MutablePomFile pom = project.getMetadata(MutablePomFile.class);
        return PackagingType.TYPE_P2_IU.equals(pom.getPackaging());
    }
}
