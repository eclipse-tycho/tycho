package org.eclipse.tycho.p2.target;

import java.io.File;

import org.eclipse.tycho.p2.metadata.IArtifactFacade;

/**
 * Key object for the cache of the Artifact->InstallableUnits contained by the
 * {@link TargetPlatformBundlePublisherService} class. This was neccessary, since the
 * {@link IArtifactFacade} is an interface, and we did not want to rely on the
 * {@link #equals(Object)} and {@link #hashCode()} implementation of the implementors.
 * 
 * @author liptak
 */
class ArtifactFacadeKey implements IArtifactFacade {
    private String groupId;
    private String artifactId;
    private String classifier;
    private String version;
    private String packagingType;
    private File file;

    public ArtifactFacadeKey(IArtifactFacade artifactFacade) {
        groupId = artifactFacade.getGroupId();
        artifactId = artifactFacade.getArtifactId();
        classifier = artifactFacade.getClassifier();
        version = artifactFacade.getVersion();
        packagingType = artifactFacade.getPackagingType();
        file = artifactFacade.getLocation();
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPackagingType() {
        return packagingType;
    }

    @Override
    public File getLocation() {
        return file;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((packagingType == null) ? 0 : packagingType.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArtifactFacadeKey other = (ArtifactFacadeKey) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (classifier == null) {
            if (other.classifier != null)
                return false;
        } else if (!classifier.equals(other.classifier))
            return false;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (packagingType == null) {
            if (other.packagingType != null)
                return false;
        } else if (!packagingType.equals(other.packagingType))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ArtifactFacadeKey [");
        if (groupId != null)
            builder.append("groupId=").append(groupId).append(", ");
        if (artifactId != null)
            builder.append("artifactId=").append(artifactId).append(", ");
        if (classifier != null)
            builder.append("classifier=").append(classifier).append(", ");
        if (version != null)
            builder.append("version=").append(version).append(", ");
        if (packagingType != null)
            builder.append("packagingType=").append(packagingType).append(", ");
        if (file != null)
            builder.append("file=").append(file);
        builder.append("]");
        return builder.toString();
    }
}
