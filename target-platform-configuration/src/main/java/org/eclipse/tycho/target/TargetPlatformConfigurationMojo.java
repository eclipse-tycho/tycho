package org.eclipse.tycho.target;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.core.TargetPlatformConfiguration.BREEHeaderSelectionPolicy;
import org.eclipse.tycho.core.TargetPlatformConfiguration.PomDependencies;
import org.eclipse.tycho.core.resolver.DefaultDependencyResolverFactory;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.p2.repository.GAV;

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
     * ⚠️ Children element <b>must</b> use <code>&lt;artifact></code> as tag name
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET)
    private GAV[] target;

    /**
     * <p>
     * Set to <code>consider</code> to consider OSGi bundles added in the
     * <code><dependencies></code> section as dependencies during dependency resolution.
     * </p>
     * <p>
     * This configuration has the following effect:
     * <ul>
     * <li>First, Maven resolves the GAV dependencies according to the normal Maven rules. This
     * results in a list of artifacts consisting of the specified artifacts and their transitive
     * Maven dependencies.</li>
     * <li>Tycho then checks each of these artifacts, and if the artifact is an OSGi bundle, it is
     * added to the target platform. Other artifacts are ignored. OSGi bundles which become part of
     * the target platform in this way are then available to resolve the project's OSGi
     * dependencies.</li>
     * </ul>
     * </p>
     * <p>
     * 📝 Tycho always attempts to resolve transitive dependencies, so if you need a POM dependency
     * in the target platform of one module, you will also need it in all downstream modules.
     * Therefore the POM dependencies (and the pomDependencies=consider configuration) typically
     * need to be added in the parent POM.
     * </p>
     */
    @Parameter(name = DefaultTargetPlatformConfigurationReader.POM_DEPENDENCIES)
    private PomDependencies pomDependencies;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.ALLOW_CONFLICTING_DEPENDENCIES, defaultValue = "false")
    private boolean allowConflictingDependences;

    /**
     * Force an execution environment for dependency resolution. If unset, the first
     * <code>targetJRE</code> available in {@link #target} is used.
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

    @Parameter(name = DefaultTargetPlatformConfigurationReader.FILTERS)
    private List<?> filters; // TODO stronger typing

    @Parameter(name = DefaultTargetPlatformConfigurationReader.DEPENDENCY_RESOLUTION)
    private Object dependencyResolution; // TODO stronger typing

    @Parameter(name = DefaultTargetPlatformConfigurationReader.INCLUDE_PACKED_ARTIFACTS)
    private boolean includePackedArtifacts;

    @Parameter(name = DefaultTargetPlatformConfigurationReader.TARGET_DEFINITION_INCLUDE_SOURCE)
    private IncludeSourceMode targetDefinionIncludeSource;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        throw new MojoFailureException(
                "This mojo isn't meant to be executed, it's only a placeholder for configuration.");
    }

}
