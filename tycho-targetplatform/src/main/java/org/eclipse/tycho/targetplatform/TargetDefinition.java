/*******************************************************************************
 * Copyright (c) 2011, 2024 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    Christoph Läubrich -  [Bug 538144] - support other target locations (Directory, Feature, Installations)
 *                          [Bug 568729] - Support new "Maven" Target location
 *                          [Bug 569481] - Support for maven target location includeSource="true" attribute
 *                          [Issue 189]  - Support multiple maven-dependencies for one target location
 *                          [Issue 194]  - Support additional repositories defined in the maven-target location
 *                          [Issue 401]  - Support nested targets
 *******************************************************************************/
package org.eclipse.tycho.targetplatform;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.tycho.IArtifactFacade;
import org.eclipse.tycho.MavenArtifactRepositoryReference;
import org.osgi.resource.Requirement;
import org.w3c.dom.Element;

// TODO javadoc
public interface TargetDefinition {

	public List<? extends Location> getLocations();

	/**
	 * Returns <code>true</code> if the target definition specifies an explicit list
	 * of bundles to include (i.e. an <code>&lt;includeBundles&gt;</code> in target
	 * definition files).
	 */
	boolean hasIncludedBundles();

	/**
	 * Returns the origin of the target definition, e.g. a file path. Used for
	 * debugging only.
	 */
	String getOrigin();

	/**
	 * Returns the value of the targetJRE in *.target file if it's a known EE name.
	 * <code>null</code> will be returned otherwise.
	 */
	String getTargetEE();

	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();

	public interface Location {

		/**
		 * Returns a description of the underlying location implementation.
		 */
		String getTypeDescription();

	}

	public enum FollowRepositoryReferences {
		DEFAULT,
		ENABLED,
		DISABLED,
	}

	public interface InstallableUnitLocation extends Location {

		public static String TYPE = "InstallableUnit";
		
		public List<? extends Repository> getRepositories();

		public List<? extends Unit> getUnits();

		public IncludeMode getIncludeMode();

		public boolean includeAllEnvironments();

		public boolean includeSource();
		
		/**
		 * Read for completeness but not used
		 */
		public boolean includeConfigurePhase();
		
		/**
		 * When {@link FollowRepositoryReferences.Default} the global {@link IncludeSourceMode} should be used instead.
		 * @return whether repository references should be used, never null
		 */
		public default FollowRepositoryReferences followRepositoryReferences() {
			return FollowRepositoryReferences.DEFAULT;
		}

		@Override
		public default String getTypeDescription() {
			return InstallableUnitLocation.TYPE;
		}

	}

	public interface MavenGAVLocation extends Location {

		public static final String TYPE = "Maven";

		enum MissingManifestStrategy {
			IGNORE, ERROR, GENERATE;
		}

		enum DependencyDepth {
			NONE, DIRECT, INFINITE;
		}

		Collection<String> getIncludeDependencyScopes();

		DependencyDepth getIncludeDependencyDepth();

		MissingManifestStrategy getMissingManifestStrategy();

		Collection<BNDInstructions> getInstructions();

		Collection<MavenDependency> getRoots();

		Collection<MavenArtifactRepositoryReference> getRepositoryReferences();

		boolean includeSource();

		Element getFeatureTemplate();

		String getLabel();

		@Override
		public default String getTypeDescription() {
			return TYPE;
		}

	}

	public interface TargetReferenceLocation extends Location {
		String getUri();
	}

	/**
	 * Implements the <a href=
	 * "https://eclipse.dev/eclipse/news/4.29/pde.php#osgi-repository-target-type">PDE
	 * repository location</a>
	 * 
	 */
	public interface RepositoryLocation extends Location {

		static final String TYPE = "Repository";

		/**
		 * @return the URI to load this repository from
		 */
		String getUri();

		/**
		 * @return the requirements that make up the content fetched from the repository
		 */
		Collection<Requirement> getRequirements();

		@Override
		default String getTypeDescription() {
			return TYPE;
		}
	}

	/**
	 * Represents the "Directory" location that either contains bundles directly or
	 * has plugins/features/binaries folders that contains the data
	 * 
	 * @author Christoph Läubrich
	 *
	 */
	public interface DirectoryLocation extends PathLocation {
	}

	/**
	 * Represents the "Profile" location that contains an eclipse-sdk or exploded
	 * eclipse product
	 * 
	 * @author Christoph Läubrich
	 *
	 */
	public interface ProfileLocation extends PathLocation {
	}

	/**
	 * represents the "Feature" location that contains a feature to include from a
	 * given installation
	 * 
	 * @author Christoph Läubrich
	 *
	 */
	public interface FeaturesLocation extends PathLocation {

		/**
		 * 
		 * @return the id of the feature to use
		 */
		String getId();

		/**
		 * 
		 * @return the version of the feature to use
		 */
		String getVersion();
	}

	/**
	 * Base interface for all Locations that are path based, the path might contains
	 * variables that need to be resolved before used as a real directory path
	 * 
	 * @author Christoph Läubrich
	 *
	 */
	public interface PathLocation extends Location {
		/**
		 * 
		 * @return the plain path as supplied by the target file
		 */
		public String getPath();
	}

	public enum IncludeMode {
		SLICER, PLANNER
	}

	public interface Repository {
		String getLocation();

		String getId();
	}

	public interface Unit {

		public String getId();

		public String getVersion();
	}

	public interface BNDInstructions {

		public String getReference();

		public Properties getInstructions();
	}

	public interface MavenDependency {

		String getGroupId();

		String getArtifactId();

		String getVersion();

		String getArtifactType();

		String getClassifier();

		boolean isIgnored(IArtifactFacade artifact);
	}

}
