package org.eclipse.tycho;

import java.util.List;

import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;

public interface Requirements {

    List<RequiredCapability> getRequiredCapabilities(IDependencyMetadata dependencyMetadata, DependencyMetadataType... types);
}
