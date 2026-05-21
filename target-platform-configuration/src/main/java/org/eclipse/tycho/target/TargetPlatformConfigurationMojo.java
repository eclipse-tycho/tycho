/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Red Hat Inc. - initial API and implementation
 *    Christoph Läubrich - [Issue 792]  - Support exclusion of certain dependencies from pom dependency consideration 
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.util.List;

import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration.BREEHeaderSelectionPolicy;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.targetplatform.TargetPlatformFilter.CapabilityPattern;

/**
 * Configures the target-platform to use in order to resolve dependencies. <br>
 * ⚠️ This mojo is actually not executable, and is only meant to host configuration
 */
@Mojo(name = "target-platform-configuration")
public class TargetPlatformConfigurationMojo extends AbstractMojo {

    /**
     * <p>
     * Target environments (os/ws/arch) to consider.
     * </p>
     * Example:
     * 
     * <pre>
     *&lt;environments&gt;
    *  &lt;environment&gt;
    *    &lt;os&gt;linux&lt;/os&gt;
    *    &lt;ws&gt;gtk&lt;/ws&gt;
    *    &lt;arch&gt;x86_64&lt;/arch&gt;
    *  &lt;/environment&gt;
    *  &lt;environment&gt;
    *    &lt;os&gt;linux&lt;/os&gt;
    *    &lt;ws&gt;gtk&lt;/ws&gt;
    *    &lt;arch&gt;ppc64le&lt;/arch&gt;
    *  &lt;/environment&gt;
    *   &lt;environment&gt;
    *    &lt;os&gt;linux&lt;/os&gt;
    *    &lt;ws&gt;gtk&lt;/ws&gt;
    *    &lt;arch&gt;aarch64&lt;/arch&gt;
    *  &lt;/environment&gt;
    *  &lt;environment&gt;
    *    &lt;os&gt;win32&lt;/os&gt;
    *    &lt;ws&gt;win32&lt;/ws&gt;
    *    &lt;arch&gt;x86_64&lt;/arch&gt;
    *  &lt;/environment&gt;
    *  &lt;environment&gt;
    *    &lt;os&gt;macosx&lt;/os&gt;
    *    &lt;ws&gt;cocoa&lt;/ws&gt;
    *    &lt;arch&gt;x86_64&lt;/arch&gt;
    *  &lt;/environment&gt;
    *  &lt;environment&gt;
    *    &lt;os&gt;macosx&lt;/os&gt;
    *    &lt;ws&gt;cocoa&lt;/ws&gt;
    *    &lt;arch&gt;aarch64&lt;/arch&gt;
    *  &lt;/environment&gt;
    *&lt;/environments&gt;
     * </pre>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.ENVIRONMENTS)
    private TargetEnvironment[] environments;

    /**
     * List of .target artifacts to use for dependency resolution.<br>
     * Could either be
     * <ul>
     * <li><code>&lt;artifact></code> to define a target GAV (either local to the reactor or a
     * remote one)</li>
     * <li><code>&lt;file></code> to define a file local to the build</li>
     * <li><code>&lt;uri></code> to define a (remote) URI that specifies a target, currently only
     * URIs that can be converted to URLs are supported (e.g. file:/.... http://..., )</li>
     * <li>{@code <location>} to define target location inline</li>
     * </ul>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET)
    private TargetParameterObject target;

    /**
     * Defines which strategy to apply to Maven dependencies.
     * <p>
     * If <code>consider</code> or <code>wrapAsBundle</code>, the effect is:
     * <ul>
     * <li>First, Maven resolves the GAV dependencies according to the normal Maven rules. This
     * results in a list of artifacts consisting of the specified artifacts and their transitive
     * Maven dependencies.</li>
     * <li>Tycho then checks each of these artifacts, and if the artifact is an OSGi bundle, it is
     * added to the target platform. Other artifacts are ignored in case of <code>consider</code>,
     * or get some OSGi metadata generated and an OSGi bundle created from them.</li>
     * <li>OSGi bundles which become part of the target platform in this way are then available to
     * resolve the project's OSGi dependencies.</li>
     * </ul>
     * </p>
     * <p>
     * 📝 Tycho always attempts to resolve transitive dependencies, so if you need a POM dependency
     * in the target platform of one module, you will also need it in all downstream modules.
     * Therefore the POM dependencies (and the pomDependencies=consider configuration) typically
     * need to be added in the parent POM.
     * </p>
     * <p>
     * If no explicit value is configured Tycho uses {@link PomDependencies#ignore} if eager
     * resolution is activated and {@link PomDependencies#consider} otherwhise.
     * </p>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.POM_DEPENDENCIES, property = DefaultTargetPlatformConfigurationReader.PROPERTY_POM_DEPENDENCIES)
    private PomDependencies pomDependencies;

    /**
     * Force an execution environment for dependency resolution. If unset, use the default JRE of
     * your computer.
     * <p>
     * Set to <code>none</code> to force the resolution to happen <b>without</b> any execution
     * environment, typically when the module is supposed to use system packages coming from some
     * dependencies (eg shipping a JRE inside products with Eclipse JustJ).
     * </p>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.EXECUTION_ENVIRONMENT)
    private String executionEnvironment;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.EXECUTION_ENVIRONMENT_DEFAULT)
    private String executionEnvironmentDefault;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.BREE_HEADER_SELECTION_POLICY)
    private BREEHeaderSelectionPolicy breeHeaderSelectionPolicy;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS, defaultValue = "true")
    private boolean resolveWithExcutionEnvironmentConstraints;

    /**
     * Configures when resolve of the project specific target platform happens. If the value is
     * <code>true</code> the project platform is computed as early as when starting the build before
     * the first mojo executes, if the value is <code>false</code> the resolving is delayed until
     * the project is actually executed, this can considerably improve your build speed in parallel
     * builds. The drawback is that there might be some tools making assumptions about the build
     * being static from the start or having "hidden" dependency chains that point back to your
     * build reactor. For these reason this can be configured here even though it is recommend to
     * always use lazy resolve for best performance and maximum of features, e.g. using mixed maven
     * builds require lazy resolving of that projects depend on the plain maven projects.
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.REQUIRE_EAGER_RESOLVE, defaultValue = "false", property = DefaultTargetPlatformConfigurationReader.PROPERTY_REQUIRE_EAGER_RESOLVE, alias = DefaultTargetPlatformConfigurationReader.PROPERTY_ALIAS_REQUIRE_EAGER_RESOLVE)
    private boolean requireEagerResolve;

    /**
     * Selectively remove content from the target platform.
     * <p>
     * This for example allows to restrict the version of a bundle, or to select one particular
     * provider for a package. Filtering is done as last step in the target platform computation, so
     * the filters apply to all sources listed above.
     * </p>
     * <p>
     * The filters will only remove content from the target platform; they will not add new content.
     * {@code dependency-resolution} should be used for addition of extra content. If you specify a
     * restriction that is not fulfilled by any of the units from the target platform sources, all
     * units that the filter applies to (i.e. units that match the filter.type, filter.id, and
     * filter.version/versionRange criteria) will be removed from the target platform.
     * </p>
     * <p>
     * Package provider restrictions work by removing all other bundles exporting the package. This
     * means that these other bundles (and the packages only exported by them) won't be available in
     * your build.
     * </p>
     * 
     * <p>
     * Example:
     * 
     * <pre>
     * &lt;plugin>
     *   &lt;groupId>org.eclipse.tycho&lt;/groupId>
     *   &lt;artifactId>target-platform-configuration&lt;/artifactId>
     *   &lt;version>${tycho-version}&lt;/version>
     *   &lt;configuration>
     *     &lt;filters>
     *       &lt;!-- example 1: restrict version of a bundle -->
     *       &lt;filter>
     *         &lt;type>eclipse-plugin&lt;/type>
     *         &lt;id>org.eclipse.osgi&lt;/id>
     *         &lt;restrictTo>
     *           &lt;versionRange>[3.6,3.7)&lt;/versionRange>
     *           &lt;!-- alternative: &lt;version> for selecting exactly one versions -->
     *         &lt;/restrictTo>
     *       &lt;/filter>
     *       &lt;!-- example 2: remove all providers of the package javax.persistence except the bundle javax.persistence -->
     *       &lt;filter>
     *         &lt;type>java-package&lt;/type>
     *         &lt;id>javax.persistence&lt;/id>
     *         &lt;restrictTo>
     *           &lt;type>eclipse-plugin&lt;/type>
     *           &lt;id>javax.persistence&lt;/id>
     *         &lt;/restrictTo>
     *       &lt;/filter>
     *       &lt;!-- example 3: work around Equinox bug 348045 -->
     *       &lt;filter>
     *         &lt;type>p2-installable-unit&lt;/type>
     *         &lt;id>org.eclipse.equinox.servletbridge.extensionbundle&lt;/id>
     *         &lt;removeAll />
     *       &lt;/filter>
     *     &lt;/filters>
     *   &lt;/configuration>
     * &lt;/plugin>
     * </pre>
     * </p>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.FILTERS)
    private List<CapabilityPattern> filters;

    /**
     * Exclusions could be used together with {@link #pomDependencies} setting to exclude certain
     * maven dependencies from being considered. This is useful for example if there is an offending
     * (transitive) dependency needed for compilation but not for the runtime that would cause
     * problems otherwise.
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.EXCLUSIONS)
    private List<Exclusion> exclusions;

    /**
     * Configure dependency resolution, for example by adding requirements.
     * 
     * Example:
     * 
     * <pre>
     * &lt;plugin>
     *   &lt;groupId>org.eclipse.tycho&lt;/groupId>
     *   &lt;artifactId>target-platform-configuration&lt;/artifactId>
     *   &lt;configuration>
     *     &lt;dependency-resolution>
     *       &lt;extraRequirements>
     *         &lt;requirement>
     *           &lt;type>eclipse-feature&lt;/type>
     *           &lt;id>example.project.feature&lt;/id>
     *           &lt;versionRange>0.0.0&lt;/versionRange>
     *         &lt;/requirement>
     *       &lt;/extraRequirements>
     *     &lt;/dependency-resolution>
     *   &lt;/configuration>
     *  &lt;/plugin>
     * </pre>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.DEPENDENCY_RESOLUTION)
    private DependencyResolutionConfiguration dependencyResolution;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET_DEFINITION_INCLUDE_SOURCE)
    private IncludeSourceMode targetDefinionIncludeSource;

    /**
     * Configures if referenced repositories should be included when fetching repositories. The
     * default is <code>include</code>. To disable the use of referenced repositories, pass
     * <code>ignore</code>.
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.REFERENCED_REPOSITORY_MODE)
    private ReferencedRepositoryMode referencedRepositoryMode;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoFailureException(
                "This mojo isn't meant to be executed, it's only a placeholder for configuration.");
    }

}
