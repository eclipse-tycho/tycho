package org.eclipse.tycho.plugins.p2.publisher;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.IDependencyMetadata;
import org.eclipse.tycho.IDependencyMetadata.DependencyMetadataType;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.UnmodifiableDependencyMetadata;
import org.eclipse.tycho.p2maven.InstallableUnitGenerator;
import org.eclipse.tycho.resolver.P2MetadataProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(PublishProductInstallableUnitProvider.HINT)
public class PublishProductMetadataProvider implements P2MetadataProvider {

    @Inject
    private InstallableUnitGenerator installableUnitGenerator;

    @Override
    public Map<String, IDependencyMetadata> getDependencyMetadata(MavenSession session, MavenProject project,
            List<TargetEnvironment> environments, OptionalResolutionAction optionalAction) {

        Set<IInstallableUnit> productUnits = PublishProductInstallableUnitProvider
                .getProductUnits(installableUnitGenerator, project);
        if (productUnits.isEmpty()) {
            return Map.of();
        }
        return Map.of(PublishProductInstallableUnitProvider.HINT,
                new UnmodifiableDependencyMetadata(productUnits, DependencyMetadataType.ADDITIONAL));
    }

}
