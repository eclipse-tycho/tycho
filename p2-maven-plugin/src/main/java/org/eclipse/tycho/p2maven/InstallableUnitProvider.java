package org.eclipse.tycho.p2maven;

import java.util.Collection;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Components implementing this interface can provide additional project units,
 * for example source features/bundles.
 */
public interface InstallableUnitProvider {

	/**
	 * Computes the {@link IInstallableUnit}s for the given maven project
	 * 
	 * @param project
	 * @param session
	 * @return the collection of units, probably empty but never <code>null</code>
	 * @throws CoreException if anything goes wrong
	 */
	Collection<IInstallableUnit> getInstallableUnits(MavenProject project, MavenSession session) throws CoreException;

}
