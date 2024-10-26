/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation 
 *******************************************************************************/
package org.eclipse.tycho.build;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.ModelVersionProcessor;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.Priority;
import org.eclipse.tycho.TychoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named("default")
@Priority(10)
public class TychoCiFriendlyVersions implements ModelVersionProcessor {

	static final String PROPERTY_FORCE_QUALIFIER = "forceContextQualifier";

	static final String PROPERTY_BUILDQUALIFIER_FORMAT = "tycho.buildqualifier.format";

	static final String BUILD_QUALIFIER = "qualifier";

	static final String MICRO_VERSION = "micro";
	static final String MINOR_VERSION = "minor";
	static final String MAJOR_VERSION = "major";
	static final String RELEASE_VERSION = "releaseVersion";

	private static final Set<String> SIMPLE_PROPERTIES = Set.of(RELEASE_VERSION, MAJOR_VERSION, MINOR_VERSION,
			MICRO_VERSION);

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Map<File, MavenProject> rawProjectCache = new ConcurrentHashMap<>();

	private final DefaultModelReader defaultModelReader;
	private final DefaultModelVersionProcessor defaultModelVersionProcessor;
	private final Map<String, BuildTimestampProvider> buildTimestampProviders;
	private final Provider<MavenSession> mavenSessionProvider;

	@Inject
	public TychoCiFriendlyVersions(DefaultModelReader defaultModelReader,
								   DefaultModelVersionProcessor defaultModelVersionProcessor,
								   Map<String, BuildTimestampProvider> buildTimestampProviders,
								   Provider<MavenSession> mavenSessionProvider) {
		this.defaultModelReader = defaultModelReader;
		this.defaultModelVersionProcessor = defaultModelVersionProcessor;
		this.buildTimestampProviders = buildTimestampProviders;
		this.mavenSessionProvider = mavenSessionProvider;
	}

	@Override
	public boolean isValidProperty(String property) {
		return defaultModelVersionProcessor.isValidProperty(property) || SIMPLE_PROPERTIES.contains(property)
				|| BUILD_QUALIFIER.equals(property);
	}

	@Override
	public void overwriteModelProperties(Properties modelProperties, ModelBuildingRequest request) {
		defaultModelVersionProcessor.overwriteModelProperties(modelProperties, request);
		for (String property : SIMPLE_PROPERTIES) {
			if (request.getSystemProperties().containsKey(property)) {
				modelProperties.put(property, request.getSystemProperties().get(property));
			}
		}
		if (request.getSystemProperties().containsKey(BUILD_QUALIFIER)) {
			modelProperties.put(BUILD_QUALIFIER, request.getSystemProperties().get(BUILD_QUALIFIER));
		} else {
			String forceContextQualifier = request.getSystemProperties().getProperty(PROPERTY_FORCE_QUALIFIER);
			if (forceContextQualifier != null) {
				modelProperties.put(BUILD_QUALIFIER,
						TychoConstants.QUALIFIER_NONE.equals(forceContextQualifier) ? "" : "." + forceContextQualifier);
			} else {
				String formatString = request.getSystemProperties().getProperty(PROPERTY_BUILDQUALIFIER_FORMAT);
				if (formatString != null) {
					Date startTime = request.getBuildStartTime();
					File pomFile = request.getPomFile();
					if (startTime != null && pomFile != null) {
						String provider = request.getSystemProperties().getProperty("tycho.buildqualifier.provider",
								"default");
						try {
							BuildTimestampProvider timestampProvider = buildTimestampProviders.get(provider);
							if (timestampProvider == null) {
								throw new MojoExecutionException("Can't find timestamp provider " + provider);
							}
							SimpleDateFormat format = new SimpleDateFormat(formatString);
							format.setTimeZone(TimeZone.getTimeZone("UTC"));
							MavenProject mavenProject = getMavenProject(pomFile);
							timestampProvider.setQuiet(true);
							try {
								Date timestamp = timestampProvider.getTimestamp(getMavenSession(request), mavenProject,
										getExecution(mavenProject));
								String qualifier = format.format(timestamp);
								modelProperties.put(BUILD_QUALIFIER, "." + qualifier);
							} finally {
								timestampProvider.setQuiet(false);
							}
						} catch (MojoExecutionException e) {
							logger.warn("Cannot use '" + provider
									+ "' as a timestamp provider for tycho-ci-friendly-versions (" + e + ")");
						}

					}
				}
			}
		}
	}

	private MojoExecution getExecution(MavenProject mavenProject) {

		Plugin projectPlugin = getPlugin("org.eclipse.tycho:tycho-packaging-plugin", mavenProject);
		if (projectPlugin == null) {
			// create a dummy
			projectPlugin = new Plugin();
			projectPlugin.setGroupId("org.eclipse.tycho");
			projectPlugin.setArtifactId("tycho-packaging-plugin");
			try {
				projectPlugin.setConfiguration(Xpp3DomBuilder.build(new StringReader(
						"<configuration><jgit.dirtyWorkingTree>ignore</jgit.dirtyWorkingTree></configuration>")));
			} catch (XmlPullParserException | IOException e) {
				projectPlugin.setConfiguration(null);
			}
		}
		return new MojoExecution(projectPlugin, "", "");
	}

	private Plugin getPlugin(String id, MavenProject mavenProject) {
		if (mavenProject == null) {
			return null;
		}
		Plugin plugin = mavenProject.getPlugin(id);
		if (plugin == null) {
			return getPlugin(id, mavenProject.getParent());
		}
		return plugin;
	}

	private MavenProject getMavenProject(File pom) {

		MavenProject project = rawProjectCache.computeIfAbsent(pom, file -> {
			// at this phase there are no projects, thats all we can offer for now...
			MavenProject mavenProject = new MavenProject();
			try {
				mavenProject.setModel(defaultModelReader.read(pom, Map.of()));
			} catch (IOException e) {
				// nothing to do here then...
			}
			mavenProject.setFile(pom);
			return mavenProject;
		});
		Model model = project.getModel();
		if (model != null) {
			Parent parent = model.getParent();
			if (parent != null) {
				File parentPom = new File(pom.getParentFile(), parent.getRelativePath());
				if (parentPom.isFile()) {
					project.setParent(getMavenProject(parentPom));
				}
			}
		}
		return project;

	}

	@SuppressWarnings("deprecation")
	private MavenSession getMavenSession(ModelBuildingRequest request) {
		MavenSession session = mavenSessionProvider.get();
		if (session != null) {
			return session;
		}
		// create a dummy session... actually time providers are not really interested
		// in *all* but very limited details
		DefaultMavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
		executionRequest.setBaseDirectory(request.getPomFile().getParentFile());
		executionRequest.setStartTime(request.getBuildStartTime());
		return new MavenSession(null, executionRequest, new DefaultMavenExecutionResult(), Collections.emptyList());
	}

}
