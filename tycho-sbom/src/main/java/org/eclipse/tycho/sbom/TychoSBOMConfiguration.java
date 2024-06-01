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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.PackagingType;

/**
 * Configuration of the {@code tycho-sbom} plugin to configure the packaging
 * types for which the {@link TychoModelConverter} is used. By default only
 * plug-ins and features are supported, but user may include additional types
 * using this class. Example:
 * 
 * <pre>
 * &lt;configuration&gt;
 *   &lt;includes&gt;
 *     &lt;include&gt;eclipse-plugin&lt;/include&gt;
 *     &lt;include&gt;eclipse-feature&lt;/include&gt;
 *     &lt;include&gt;eclipse-repository&lt;/include&gt;
 *   &lt;/includes&gt;
 * &lt;/configuration>&gt;
 * </pre>
 */
public class TychoSBOMConfiguration {
	private static final Set<String> DEFAULT_TYPES = Set.of(PackagingType.TYPE_ECLIPSE_FEATURE,
			PackagingType.TYPE_ECLIPSE_PLUGIN);
	private static final String KEY_INCLUDES = "includes";
	/**
	 * Contains all packaging types for which the {@link TychoModelConverter} should
	 * be used. Initialized with {@link #DEFAULT_TYPES} if no other configuration is
	 * specified.
	 */
	private Set<String> includes = DEFAULT_TYPES;
	
	/**
	 * A default implementation that is used when used outside of a Maven project.
	 */
	public TychoSBOMConfiguration() {
		// no-op
	}
	
	/**
	 * Initializes the configuration based on the configuration of the
	 * {@code tycho-sbom} plugin. If no configuration exists, this configuration is
	 * initialized with its default values.
	 */
	public TychoSBOMConfiguration(MavenProject currentProject) {
		Plugin plugin = currentProject.getPlugin("org.eclipse.tycho:tycho-sbom");
		if (plugin != null && plugin.getConfiguration() instanceof Xpp3Dom root) {
			readIncludes(root.getChild(KEY_INCLUDES));
		}
	}
	
	private void readIncludes(Xpp3Dom parent) {
		if (parent == null) {
			return;
		}
		// Overwrite default configuration
		includes = Arrays.stream(parent.getChildren()) //
				.map(Xpp3Dom::getValue) //
				.collect(Collectors.toUnmodifiableSet());
	}
	
	/**
	 * Returns all packaging types for which the {@link TychoModelConverter} should
	 * be used.
	 * 
	 * @return An unmodifiable list of {@link String}s.
	 */
	public Set<String> getIncludedPackagingTypes() {
		// Set is already immutable
		return includes;
	}
}
