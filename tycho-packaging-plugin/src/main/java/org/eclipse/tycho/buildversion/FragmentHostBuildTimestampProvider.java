/*******************************************************************************
 * Copyright (c) 20024 Christoph Läubrich and others.
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
package org.eclipse.tycho.buildversion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencyArtifacts;
import org.eclipse.tycho.build.BuildTimestampProvider;
import org.eclipse.tycho.core.BundleProject;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.helper.PluginConfigurationHelper;
import org.eclipse.tycho.helper.PluginConfigurationHelper.Configuration;
import org.osgi.framework.Constants;

/**
 * Build timestamp provider that inherits the timestamp of the fragment host
 */
@Singleton
@Named(FragmentHostBuildTimestampProvider.ROLE_HINT)
public class FragmentHostBuildTimestampProvider implements BuildTimestampProvider {

	static final String ROLE_HINT = "fragment-host";

	@Inject
	private TychoProjectManager projectManager;

	@Inject
	private Logger logger;

	@Inject
	private TimestampFinder timestampFinder;

	@Inject
	private PluginConfigurationHelper configurationHelper;

	@Override
	public Date getTimestamp(MavenSession session, MavenProject project, MojoExecution execution) {
		Optional<TychoProject> tychoProject = projectManager.getTychoProject(project);
		Exception exception = null;
		if (tychoProject.isPresent()) {
			if (tychoProject.get() instanceof BundleProject bundle) {
				String fragmentHost = bundle.getManifestValue(Constants.FRAGMENT_HOST, project);
				if (fragmentHost != null) {
					try {
						ManifestElement[] header = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, fragmentHost);
						for (ManifestElement element : header) {
							DependencyArtifacts dependencyArtifacts = projectManager.getDependencyArtifacts(project)
									.get();
							ArtifactDescriptor descriptor = dependencyArtifacts.getArtifact(
									ArtifactType.TYPE_ECLIPSE_PLUGIN, element.getValue(),
									element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));
							if (descriptor != null) {

								Configuration configuration = configurationHelper.getConfiguration();
								Optional<String> formatString = configuration
										.getString(BuildQualifierMojo.PARAMETER_FORMAT);
								SimpleDateFormat format = formatString.map(SimpleDateFormat::new)
										.orElseGet(() -> new SimpleDateFormat(BuildQualifierMojo.DEFAULT_DATE_FORMAT));
								format.setTimeZone(BuildQualifierMojo.TIME_ZONE);
								Date date = timestampFinder.findByDescriptor(descriptor, format);
								if (date != null) {
									return date;
								}
							}
						}
					} catch (Exception e) {
						exception = e;
					}
				}
			}
		}
		logger.warn("Can't determine fragment host, fallback to default.", exception);
		return session.getStartTime();
	}

	@Override
	public void setQuiet(boolean quiet) {

	}
}
