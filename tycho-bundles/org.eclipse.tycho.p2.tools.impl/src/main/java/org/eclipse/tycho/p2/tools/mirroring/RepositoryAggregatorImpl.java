package org.eclipse.tycho.p2.tools.mirroring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.MavenLogger;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.target.P2TargetPlatform;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;
import org.eclipse.tycho.p2.tools.mirroring.facade.RepositoryAggregator;
import org.eclipse.tycho.p2.util.resolution.AbstractResolutionStrategy;
import org.eclipse.tycho.p2.util.resolution.ResolutionDataImpl;
import org.eclipse.tycho.p2.util.resolution.SlicerResolutionStrategy;
import org.eclipse.tycho.repository.p2base.artifact.provider.CompositeArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.IRawArtifactProvider;
import org.eclipse.tycho.repository.p2base.artifact.provider.streaming.ArtifactSinkFactory;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.repository.registry.AggregationRepository;
import org.eclipse.tycho.repository.registry.ReactorRepositoryManager;

public class RepositoryAggregatorImpl implements RepositoryAggregator {

    // XXX inject
    private MavenLogger logger;
    private ReactorRepositoryManager reactorRepositoryManager;

    @Override
    public void mirrorReactor(ReactorProject project, DestinationRepositoryDescriptor destination,
            Collection<DependencySeed> dependencySeeds, BuildContext context, boolean includeAllDependencies,
            boolean includePacked, Map<String, String> filterProperties) {

        // XXX copy signature change from product publishing change
        P2TargetPlatform targetPlatform = (P2TargetPlatform) reactorRepositoryManager.getFinalTargetPlatform(project);

        ResolutionDataImpl resolutionData = new ResolutionDataImpl(targetPlatform.getEEResolutionHints());
        if (filterProperties != null) {
            // TODO tested?
            resolutionData.setAdditionalFilterProperties(filterProperties);
        }

        List<IInstallableUnit> availableIUs = new ArrayList<IInstallableUnit>();
        availableIUs.addAll(targetPlatform.getInstallableUnits());
        PublishingRepository projectResults = reactorRepositoryManager.getPublishingRepository(project.getIdentities());
        availableIUs.addAll(projectResults.getMetadataRepository().query(QueryUtil.ALL_UNITS, null).toSet()); // TODO test e.g. with additional p2 IUs
        resolutionData.setAvailableIUs(availableIUs);

        resolutionData.setRootIUs(toInstallableUnitList(dependencySeeds));

        AbstractResolutionStrategy strategy;
        // TODO test
//            if (includeAllDependencies) {
//                strategy = new ProjectorResolutionStrategy(logger);
//            } else {
        strategy = new SlicerResolutionStrategy(logger, false);
//            }
        strategy.setData(resolutionData);

        AggregationRepository aggregationResult = reactorRepositoryManager.createAggregationRepository(project
                .getBuildDirectory().getChild("repository"));

        HashSet<IInstallableUnit> result = new HashSet<IInstallableUnit>();
        for (TargetEnvironment environment : context.getEnvironments()) {
            Map<String, String> filter = new HashMap<String, String>();
            addFilterForFeatureJARs(filter);
            filter.putAll(environment.toFilterProperties());

            result.addAll(strategy.resolve(environment, null));
        }

        aggregationResult.getMetadataRepository().addInstallableUnits(result);

        IRawArtifactProvider artifactSource = new CompositeArtifactProvider(projectResults.getArtifacts(),
                targetPlatform.getArtifacts());
        IArtifactRepository artifactRepository = aggregationResult.getArtifactRepository();
        for (IInstallableUnit unit : result) {
            for (IArtifactKey artifactKey : unit.getArtifacts()) {
                for (IArtifactDescriptor artifactDescriptor : artifactSource.getArtifactDescriptors(artifactKey)) {
                    // TODO only mirror certain artifact types
//                    if (ArtifactTransferPolicy.isCanonicalFormat(artifactDescriptor))
                    artifactSource.getArtifact(
                            ArtifactSinkFactory.rawWriteToStream(artifactDescriptor,
                                    artifactRepository.getOutputStream(artifactDescriptor)), null);
                }
            }
        }
    }

    /**
     * Set filter value so that the feature JAR units and artifacts are included when mirroring.
     */
    private static void addFilterForFeatureJARs(Map<String, String> filter) {
        filter.put("org.eclipse.update.install.features", "true");
    }

    private static List<IInstallableUnit> toInstallableUnitList(Collection<DependencySeed> seeds) {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>(seeds.size());
        for (DependencySeed seed : seeds) {
//            if (seed.getInstallableUnit() == null)                // TODO 372780 make sure this is no longer the case
            result.add((IInstallableUnit) seed.getInstallableUnit());
        }
        return result;
    }

}
