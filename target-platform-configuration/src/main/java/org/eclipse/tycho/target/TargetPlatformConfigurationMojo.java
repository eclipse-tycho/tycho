package org.eclipse.tycho.target;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.TargetPlatformConfiguration.BREEHeaderSelectionPolicy;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.shared.TargetEnvironment;

/**
 * Configures the target-platform to use in order to resolve dependencies. <br>
 * ⚠️ This mojo is actually not executable, and is only meant to host configuration
 */
@Mojo(name = "target-platform-configuration")
public class TargetPlatformConfigurationMojo extends AbstractMojo {

    /**
     * Target environments (os/ws/arch) to consider.
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.ENVIRONMENTS)
    private TargetEnvironment[] environments;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.RESOLVER, defaultValue = DefaultDependencyResolverFactory.DEFAULT_RESOLVER_HINT)
    private String resolver;

    /**
     * List of .target artifacts to use for dependency resolution.<br>
     * Could either be
     * <ul>
     * <li><code>&lt;artifact></code> to define a target GAV (either local to the reactor or a
     * remote one)</li>
     * <li><code>&lt;file></code> to define a file local to the build</li>
     * <li><code>&lt;uri></code> to define a (remote) URI that specifies a target, currently only
     * URIs that can be converted to URLs are supported (e.g. file:/.... http://..., )</li>
     * </ul>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET)
    private Xpp3Dom target;

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
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.POM_DEPENDENCIES, defaultValue = "ignore")
    private PomDependencies pomDependencies;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.ALLOW_CONFLICTING_DEPENDENCIES, defaultValue = "false")
    private boolean allowConflictingDependences;

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
     * &lt;plugin> &lt;groupId>org.eclipse.tycho&lt;/groupId>
     * &lt;artifactId>target-platform-configuration&lt;/artifactId>
     * &lt;version>${tycho-version}&lt;/version> &lt;configuration> &lt;filters>
     * 
     * &lt;!-- example 1: restrict version of a bundle --> &lt;filter>
     * &lt;type>eclipse-plugin&lt;/type> &lt;id>org.eclipse.osgi&lt;/id> &lt;restrictTo>
     * &lt;versionRange>[3.6,3.7)&lt;/versionRange> &lt;!-- alternative: &lt;version> for selecting
     * exactly one versions --> &lt;/restrictTo> &lt;/filter>
     * 
     * &lt;!-- example 2: remove all providers of the package javax.persistence except the bundle
     * javax.persistence --> &lt;filter> &lt;type>java-package&lt;/type>
     * &lt;id>javax.persistence&lt;/id> &lt;restrictTo> &lt;type>eclipse-plugin&lt;/type>
     * &lt;id>javax.persistence&lt;/id> &lt;/restrictTo> &lt;/filter>
     * 
     * &lt;!-- example 3: work around Equinox bug 348045 --> &lt;filter>
     * &lt;type>p2-installable-unit&lt;/type>
     * &lt;id>org.eclipse.equinox.servletbridge.extensionbundle&lt;/id> &lt;removeAll />
     * &lt;/filter> &lt;/filters> &lt;/configuration> &lt;/plugin>
     * 
     * <pre>
     * </p>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.FILTERS)
    private List<?> filters; //TODO stronger typing?

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
    private Object dependencyResolution; // TODO stronger typing

    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET_DEFINITION_INCLUDE_SOURCE)
    private IncludeSourceMode targetDefinionIncludeSource;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoFailureException(
                "This mojo isn't meant to be executed, it's only a placeholder for configuration.");
    }

}
