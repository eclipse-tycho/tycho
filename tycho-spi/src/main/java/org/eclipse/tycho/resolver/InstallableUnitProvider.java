package org.eclipse.tycho.resolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Components implementing this interface can provide additional project units, for example source
 * features/bundles.
 */
public interface InstallableUnitProvider {

    /**
     * Computes the {@link IInstallableUnit}s for the given maven project
     * 
     * @param project
     * @param session
     * @return the collection of units, probably empty but never <code>null</code>
     * @throws CoreException
     *             if anything goes wrong
     */
    Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session) throws CoreException;

    static Collection<IInstallableUnit> createIU(Stream<IRequirement> requirements, String idPrefix) {
        return createIU(requirements.toList(), idPrefix);
    }

    static Collection<IInstallableUnit> createIU(Collection<IRequirement> requirements, String idPrefix) {
        if (requirements.isEmpty()) {
            return Collections.emptyList();
        }
        InstallableUnitDescription result = new MetadataFactory.InstallableUnitDescription();
        result.setId(idPrefix + "-" + UUID.randomUUID());
        result.setVersion(Version.createOSGi(0, 0, 0, String.valueOf(System.currentTimeMillis())));
        result.addRequirements(requirements);
        return List.of(MetadataFactory.createInstallableUnit(result));
    }
}
