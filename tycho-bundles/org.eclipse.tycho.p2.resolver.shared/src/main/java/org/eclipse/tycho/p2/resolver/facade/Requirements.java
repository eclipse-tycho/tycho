package org.eclipse.tycho.p2.resolver.facade;

import java.util.List;

import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.RequiredCapability;

public interface Requirements {

    List<RequiredCapability> getRequiredCapabilities(IDependencyMetadata dependencyMetadata, DependencyMetadataType... types);
}
