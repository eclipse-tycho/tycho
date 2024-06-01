/*******************************************************************************
 * Copyright (c) 2024 Patrick Ziegler and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.sbom;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.cyclonedx.maven.DefaultModelConverter;
import org.cyclonedx.maven.ModelConverter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.maven.TychoReactorReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.target.ArtifactTypeHelper;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.targetplatform.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.targetplatform.TargetDefinition.Location;
import org.eclipse.tycho.targetplatform.TargetDefinition.Repository;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom implementation of the CycloneDX model converter with support for both
 * Maven and p2 artifacts. The generated PURL is usually of the form:
 * 
 * <pre>
 * pkg:/p2/&lt;id&gt;@&lt;version&gt;?classifier=&lt;classifier&gt;&amp;location=&lt;download-url&gt;
 * </pre>
 * 
 * This converter can be used with the {@code cyclonedx-maven-plugin} by adding
 * it as a dependency as follows:
 * 
 * <pre>
 * &lt;plugin>
 *   &lt;groupId&gt;org.cyclonedx&lt;/groupId&gt;
 *   &lt;artifactId&gt;cyclonedx-maven-plugin&lt;/artifactId&gt;
 *   &lt;dependencies&gt;
 *     &lt;dependency&gt;
 *       &lt;groupId&gt;org.eclipse.tycho.extras&lt;/groupId&gt;
 *       &lt;artifactId&gt;tycho-sbom&lt;/artifactId&gt;
 *     &lt;/dependency&gt;
 *   &lt;/dependencies&gt;
 * &lt;/plugin&gt;
 * </pre>
 */
@Component(role = ModelConverter.class)
public class TychoModelConverter extends DefaultModelConverter {
	private static final String KEY_CONTEXT = TychoSBOMConfiguration.class.toString();
	private static final Logger LOG = LoggerFactory.getLogger(TychoModelConverter.class);

	@Inject
	private P2RepositoryManager repositoryManager;

	@Inject
	private TychoProjectManager projectManager;

	@Inject
	private TychoReactorReader reactorReader;

	@Inject
	private LegacySupport legacySupport;

	@Override
	public String generatePackageUrl(org.apache.maven.artifact.Artifact mavenArtifact) {
		Artifact artifact = RepositoryUtils.toArtifact(mavenArtifact);
		return generatePackageUrl(artifact, true, true, () -> super.generatePackageUrl(mavenArtifact));
	}

	@Override
	public String generatePackageUrl(Artifact artifact) {
		return generatePackageUrl(RepositoryUtils.toArtifact(artifact));
	}

	@Override
	public String generateVersionlessPackageUrl(org.apache.maven.artifact.Artifact mavenArtifact) {
		Artifact artifact = RepositoryUtils.toArtifact(mavenArtifact);
		return generatePackageUrl(artifact, false, true, () -> super.generateVersionlessPackageUrl(mavenArtifact));
	}

	@Override
	public String generateVersionlessPackageUrl(Artifact artifact) {
		return generateVersionlessPackageUrl(artifact);
	}

	@Override
	public String generateClassifierlessPackageUrl(Artifact artifact) {
		return generatePackageUrl(artifact, true, false, () -> super.generateClassifierlessPackageUrl(artifact));
	}

	/**
	 * Calculates the package URL for the given artifact. For OSGi artifacts, the
	 * {@code p2} type is chosen, otherwise {@code maven}.
	 * 
	 * @param artifact       One of the artifacts available in the current reactor
	 *                       build.
	 * @param withVersion    Whether the version should be included in the PURL. For
	 *                       OSGi bundles, the {@code -SNAPSHOT} is replaced by the
	 *                       actual build qualifier.
	 * @param withClassifier Whether the classifier should be included in the PURL.
	 * @param fallback       The method for generating the Maven PURL, in case the
	 *                       artifact is not an OSGi bundle.
	 * @return
	 */
	private String generatePackageUrl(Artifact artifact, boolean withVersion, boolean withClassifier,
			Supplier<String> fallback) {
		TychoSBOMConfiguration sbomConfig = getOrCreateCurrentProjectConfiguration();
		if (sbomConfig.getIncludedPackagingTypes().contains(reactorReader.getPackagingType(artifact))) {
			ArtifactKey artifactKey = getQualifiedArtifactKey(artifact);
			IArtifactKey p2artifactKey = ArtifactTypeHelper.toP2ArtifactKey(artifactKey);
			boolean isReactorProject = reactorReader.getTychoReactorProject(artifact).isPresent();
			if (p2artifactKey != null) {
				String p2purl = generateP2PackageUrl(p2artifactKey, withVersion, withClassifier, isReactorProject);
				if (p2purl != null) {
					return p2purl;
				}
			}
		}
		return fallback.get();
	}

	/**
	 * Creates the actual package URL for the p2 artifact key, using the following
	 * format:
	 * <blockquote>scheme:type/namespace/name@version?qualifiers#subpath</blockquote>
	 * The scheme and type of the artifact are always {@code pkg} and {@code p2},
	 * respectively. The namespace is optional and not used. The name and version
	 * corresponds to {@link IArtifactKey#getId()} and
	 * {@link ArtifactKey#getVersion()}. The qualifier contains the optional key
	 * {@code classifier} with value {@link IArtifactKey#getClassifier()}and the
	 * mandatory key {@code location} with the percentage-encoded repository URL
	 * containing this artifact.
	 * 
	 * @param p2artifactKey    One of the p2 artifacts available in the current
	 *                         reactor build.
	 * @param withVersion      Whether the version should be included in the PURL.
	 *                         For OSGi bundles, the {@code -SNAPSHOT} is replaced
	 *                         by the actual build qualifier.
	 * @param withClassifier   Whether the classifier should be included in the
	 *                         PURL.
	 * @param isReactorProject if {@code true}, then the given artifact key
	 *                         corresponds to a reactor project.
	 * @return
	 * @see <a href=
	 *      "https://github.com/package-url/purl-spec/blob/master/PURL-TYPES.rst">here</a>
	 */
	/* package */ String generateP2PackageUrl(IArtifactKey p2artifactKey, boolean withVersion, boolean withClassifier,
			boolean isReactorProject) {
		String location = getRepositoryLocation(p2artifactKey, isReactorProject);
		if (location == null) {
			LOG.warn("Unknown p2 repository for artifact: " + p2artifactKey.getId());
			return null;
		}
		String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
		StringBuilder builder = new StringBuilder();
		builder.append("pkg:p2/");
		builder.append(p2artifactKey.getId());
		if (withVersion) {
			builder.append('@');
			builder.append(p2artifactKey.getVersion());
		}
		builder.append('?');
		if (withClassifier) {
			builder.append("classifier=");
			builder.append(p2artifactKey.getClassifier());
			builder.append('&');
		}
		builder.append("location=");
		builder.append(encodedLocation);
		return builder.toString();
	}

	/**
	 * Calculates the first repository from which the given artifact can be
	 * downloaded. If the artifact is part of the local reactor build, the URL
	 * specified in the {@code tycho.sbom.url} property is returned (as it has not
	 * yet been deployed to any update site). If the artifact is not available on
	 * any of the repositories, {@code null} is returned.
	 * 
	 * @param p2artifactKey    The P2 coordinates of the artifact. Used to check
	 *                         whether the artifact is available on one of the p2
	 *                         update sites.
	 * @param isReactorProject if {@code true}, then the given artifact key
	 *                         corresponds to a reactor project.
	 * @return The base URl of the repository or {@code null}, if the artifact isn't
	 *         hosted on any known repository.
	 */
	private final String getRepositoryLocation(IArtifactKey p2artifactKey, boolean isReactorProject) {
		MavenSession currentSession = legacySupport.getSession();
		if (currentSession == null) {
			LOG.error("Maven session couldn't be found.");
			return null;
		}

		MavenProject currentProject = currentSession.getCurrentProject();

		// Artifacts from the current reactor build haven't been deployed yet. Use a
		// build property to determine which repository they are published to.
		if (isReactorProject) {
			String defaultUrl = currentProject.getProperties().getProperty("tycho.sbom.url");
			if (defaultUrl == null) {
				LOG.error("'tycho.sbom.url' property not set.");
			}
			return defaultUrl;
		}

		// Iterate over all p2 repository and return the first one containing the
		// artifact. Note that the location might be arbitrary, if the artifact is
		// contained by multiple repositories.
		for (Repository repository : getTargetRepositories(currentProject)) {
			String id = repository.getId();
			URI location = URI.create(repository.getLocation());
			MavenRepositoryLocation mavenRepository = new MavenRepositoryLocation(id, location);
			try {
				IArtifactRepository artifactRepository = repositoryManager.getArtifactRepository(mavenRepository);
				if (artifactRepository.contains(p2artifactKey)) {
					return repository.getLocation();
				}
			} catch (ProvisionException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Returns the Eclipse/OSGi {@link ArtifactKey} of the given artifact. For
	 * reactor projects, the (optional) {@code -SNAPSHOT} of suffix of the version
	 * string is replaced by its expanded version.
	 * 
	 * @param artifact A Maven artifact with valid GAV
	 * @return The Eclipse/OSGi artifact key of the given artifact.
	 */
	private ArtifactKey getQualifiedArtifactKey(Artifact artifact) {
		String expandedVersion = artifact.getVersion();

		MavenProject mavenProject = reactorReader.getTychoReactorProject(artifact).orElse(null);
		if (mavenProject != null) {
			ReactorProject reactorProject = DefaultReactorProject.adapt(mavenProject);
			if (reactorProject != null) {
				expandedVersion = reactorProject.getExpandedVersion();
			} else {
				LOG.error(mavenProject + " is not a Tycho project.");
			}
		}

		String type = reactorReader.getPackagingType(artifact);
		return new DefaultArtifactKey(type, artifact.getArtifactId(), expandedVersion);
	}

	/**
	 * Returns a list of all target repositories which are accessible by the given
	 * Maven project. All IUs required by this proejct should be accessible via one
	 * of those repositories.
	 * 
	 * @param currentProject The current project of the reactor build, for which the
	 *                       SBOM is generated.
	 * @return An unmodifiable list of all target repositories.
	 */
	private List<Repository> getTargetRepositories(MavenProject currentProject) {
		TargetPlatformConfiguration targetConfiguration = projectManager.getTargetPlatformConfiguration(currentProject);
		List<Repository> p2repositories = new ArrayList<>();

		for (TargetDefinitionFile targetFile : targetConfiguration.getTargets()) {
			for (Location location : targetFile.getLocations()) {
				if (location instanceof InstallableUnitLocation iuLocation) {
					p2repositories.addAll(iuLocation.getRepositories());
				}
			}
		}

		return Collections.unmodifiableList(p2repositories);
	}
	
	/**
	 * The is created lazily based on the plugin configuration of this SBOM project.
	 * If is converter is used outside a project, a default configuration is
	 * returned. If no configuration has been created for the current project, a new
	 * instance is created and stored as context value, which is then returned on
	 * successive calls.
	 * 
	 * @return The SBOM configuration of the current project. Never {@code null}.
	 */
	private synchronized TychoSBOMConfiguration getOrCreateCurrentProjectConfiguration() {
		MavenProject currentProject = legacySupport.getSession().getCurrentProject();
		if (currentProject == null) {
			return new TychoSBOMConfiguration();
		}
		TychoSBOMConfiguration projectConfig = (TychoSBOMConfiguration) currentProject.getContextValue(KEY_CONTEXT);
		if (projectConfig == null) {
			projectConfig = new TychoSBOMConfiguration(currentProject);
			currentProject.setContextValue(KEY_CONTEXT, projectConfig);
		}
		return projectConfig;
	}
}
