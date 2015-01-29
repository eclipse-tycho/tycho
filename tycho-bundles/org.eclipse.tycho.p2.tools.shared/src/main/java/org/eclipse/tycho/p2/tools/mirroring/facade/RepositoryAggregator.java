package org.eclipse.tycho.p2.tools.mirroring.facade;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.DestinationRepositoryDescriptor;

public interface RepositoryAggregator {

    /**
     * Copies the given installable units and their dependencies into the p2 repository at the
     * destination location. By default this only includes the units and their dependencies with
     * strict versions (i.e. included content). Optionally, all transitive dependencies of the given
     * units are also copied, if includeAllDependencies is set to <code>true</code>.
     * 
     * @param reactorProject
     *            TODO
     * @param destination
     *            The p2 repository that shall be written to. The location must be a directory,
     *            which may be empty. Existing content is not overwritten but is appended to.
     * @param seeds
     *            The dependency seeds that span the content to be copied. Note that the installable
     *            units obtained from the seeds are written into the destination p2 repository
     *            without checking if they are actually present in the source repositories.
     *            Therefore only units from the source repositories should be passed via this
     *            parameter.
     * @param context
     *            Build context information; in particular this parameter defines a filter for
     *            environment specific installable units
     * @param includeAllDependencies
     *            Whether to include all transitive dependencies
     * @param includePacked
     *            Whether to include packed artifacts
     * @param filterProperties
     *            additional filter properties to be set in the p2 slicing options. May be
     *            <code>null</code>
     */
    public void mirrorReactor(ReactorProject reactorProject, DestinationRepositoryDescriptor destination,
            Collection<DependencySeed> seeds, BuildContext context, boolean includeAllDependencies,
            boolean includePacked, Map<String, String> filterProperties);

}
