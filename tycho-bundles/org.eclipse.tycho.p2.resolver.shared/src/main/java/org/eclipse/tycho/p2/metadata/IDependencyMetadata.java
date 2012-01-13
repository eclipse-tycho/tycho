package org.eclipse.tycho.p2.metadata;

import java.util.Set;

public interface IDependencyMetadata {

    Set<Object /* IInstallableUnit */> getMetadata(boolean primary);

    Set<Object /* IInstallableUnit */> getMetadata();

}
