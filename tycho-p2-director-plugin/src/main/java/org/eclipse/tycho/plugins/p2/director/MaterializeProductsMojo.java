/*******************************************************************************
 * Copyright (c) 2010, 2022 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.director;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.ExecutionEnvironment;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.ee.impl.StandardEEResolutionHints;
import org.eclipse.tycho.core.shared.StatusTool;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;
import org.eclipse.tycho.p2tools.RepositoryReferenceTool;
import org.eclipse.tycho.p2tools.copiedfromp2.PhaseSetFactory;
import org.eclipse.tycho.plugins.p2.director.runtime.StandaloneDirectorRuntimeFactory;

/**
 * <p>
 * Creates product installations for the products defined in the project.
 * </p>
 */
@Mojo(name = "materialize-products", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public final class MaterializeProductsMojo extends AbstractProductMojo {
    private static final Object LOCK = new Object();

    public enum InstallationSource {
        targetPlatform, repository
    }

    public enum DirectorRuntimeType {
        internal, standalone
    }

    /**
     * <p>
     * Comma-separated list of profile names to be published. Examples: JavaSE-11, JavaSE-17,
     * JavaSE-18.
     * 
     * If not given, all current available JavaSE profiles with version >= 11 are used.
     * </p>
     */
    @Parameter
    private String EEProfiles;

    @Component
    private MojoExecution execution;

    @Component
    private RepositoryReferenceTool repositoryReferenceTool;

    @Component
    private StandaloneDirectorRuntimeFactory standaloneDirectorFactory;

    @Component
    private ToolchainManager toolchainManager;

    @Component
    private Logger logger;

    @Component
    DirectorRuntime director;

    /**
     * The name of the p2 profile to be created.
     */
    @Parameter(defaultValue = TychoConstants.DEFAULT_PROFILE)
    private String profile;

    // TODO 405785 the syntax of this parameter doesn't work well with configuration inheritance; replace with new generic envSpecificConfiguration parameter syntax
    @Parameter
    private List<ProfileName> profileNames;

    /**
     * Include the feature JARs in installation. (Technically, this sets the property
     * <code>org.eclipse.update.install.features</code> to <code>true</code> in the p2 profile.)
     */
    @Parameter(defaultValue = "true")
    private boolean installFeatures;

    /**
     * Include the sources of JARs in installation. (Technically, this sets the property
     * <code>org.eclipse.update.install.sources</code> to <code>true</code> in the p2 profile.)
     */
    @Parameter(defaultValue = "false")
    private boolean installSources;

    /**
     * Additional profile properties to set when materializing the product
     */
    @Parameter
    private Map<String, String> profileProperties;

    /**
     * Source repositories to be used in the director calls. Can be:
     * <ul>
     * <li><code>targetPlatform</code> - to use the target platform as source (default)</li>
     * <li><code>repository</code> - to use the p2 repository in <code>target/repository/</code> as
     * source. With this option, the build implicitly verifies that it would also be possible to
     * install the product from that repository with an external director application.
     * </ul>
     */
    @Parameter(defaultValue = "targetPlatform")
    private InstallationSource source;

    /**
     * Runtime in which the director application is executed. Can be:
     * <ul>
     * <li><code>internal</code> - to use the director application from Tycho's embedded OSGi
     * runtime (default)</li>
     * <li><code>standalone</code> - to create and use a stand-alone installation of the director
     * application. This option is needed if the product to be installed includes artifacts with
     * meta-requirements (e.g. to a non-standard touchpoint action). Requires that the
     * <code>source</code> parameter is set to <code>repository</code>.
     * </ul>
     */
    @Parameter(defaultValue = "internal")
    private DirectorRuntimeType directorRuntime;

    /**
     * If a product requires JustJ this repository is automatically added as part of the product
     * assembly, if not this is just ignored. To disable this feature one can set the configuration
     * to an empty value
     */
    @Parameter(defaultValue = "https://download.eclipse.org/justj/jres")
    private String productRepository;

    /**
     * Controls if products are allowed to be build in parallel
     */
    @Parameter
    private boolean parallel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProductConfig productConfig = getProductConfig();
        List<Product> products = productConfig.getProducts();
        if (products.isEmpty()) {
            getLog().info("No product definitions found, nothing to do");
            return;
        }
        DirectorRuntime director = getDirectorRuntime();
        RepositoryReferences sources = getSourceRepositories();
        if (productRepository != null && !productRepository.isBlank()) {
            for (Product product : products) {
                if (requiresJustJ(productConfig, product)) {
                    try {
                        sources.addRepository(new URI(productRepository));
                    } catch (URISyntaxException e) {
                        throw new MojoFailureException("<productRepository> contains invalid URI: " + productRepository,
                                e);
                    }
                    break;
                }
            }
        }
        if (parallel) {
            ExecutorService executorService = Executors.newWorkStealingPool();
            ExecutorCompletionService<Void> service = new ExecutorCompletionService<>(executorService);
            try {
                int tasks = 0;
                for (Product product : products) {
                    for (TargetEnvironment env : getEnvironments()) {
                        service.submit(() -> {
                            buildProduct(director, sources, product, env);
                            return null;
                        });
                        tasks++;
                    }
                }
                for (int i = 0; i < tasks; i++) {
                    try {
                        service.take().get();
                    } catch (InterruptedException e) {
                        return;
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException rte) {
                            throw rte;
                        }
                        if (cause instanceof MojoFailureException mfe) {
                            throw mfe;
                        }
                        if (cause instanceof MojoExecutionException mee) {
                            throw mee;
                        }
                        throw new MojoFailureException("internal error", e);
                    }
                }
            } finally {
                executorService.shutdown();
            }
        } else {
            //all one by one...
            synchronized (LOCK) {
                for (Product product : products) {
                    for (TargetEnvironment env : getEnvironments()) {
                        buildProduct(director, sources, product, env);
                    }
                }
            }
        }
    }

    private boolean requiresJustJ(ProductConfig productConfig, Product product) {
        for (DependencySeed seed : productConfig.getProjectSeeds()) {
            if (ArtifactType.TYPE_ECLIPSE_PRODUCT.equals(seed.getType()) && product.getId().equals(seed.getId())) {
                IInstallableUnit installableUnit = seed.getInstallableUnit();
                for (IRequirement requirement : installableUnit.getRequirements()) {
                    if (requirement instanceof IRequiredCapability cap) {
                        if (TychoConstants.NAMESPACE_JUSTJ.equals(cap.getNamespace())
                                && cap.getName().startsWith(TychoConstants.NAME_JUSTJ_JRE)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void buildProduct(DirectorRuntime director, RepositoryReferences sources, Product product,
            TargetEnvironment env) throws MojoFailureException {
        DirectorRuntime.Command command = director
                .newInstallCommand(execution.getExecutionId() + " - " + product.getId() + " - " + env);
        command.setPhaseSet(
                PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] { PhaseSetFactory.PHASE_CHECK_TRUST }));
        File destination = getProductMaterializeDirectory(product, env);
        String rootFolder = product.getRootFolder(env.getOs());
        if (rootFolder != null && !rootFolder.isEmpty()) {
            destination = new File(destination, rootFolder);
        }
        List<IInstallableUnit> eeUnits = new ArrayList<>();
        for (String profile : getProductEEProfiles()) {
            profile = profile.trim();
            if (profile.isEmpty()) {
                continue;
            }
            ExecutionEnvironment ee = ExecutionEnvironmentUtils.getExecutionEnvironment(profile, toolchainManager,
                    getSession(), logger);
            StandardEEResolutionHints hints = new StandardEEResolutionHints(ee);
            eeUnits.addAll(hints.getMandatoryUnits());
        }
        command.setEEUnits(eeUnits);
        command.setBundlePool(getProductBundlePoolDirectory(product));
        command.addMetadataSources(sources.getMetadataRepositories());
        command.addArtifactSources(sources.getArtifactRepositories());
        command.addUnitToInstall(product.getId());
        for (DependencySeed seed : product.getAdditionalInstallationSeeds()) {
            command.addUnitToInstall(seed);
        }
        command.setDestination(destination);
        command.setProfileName(ProfileName.getNameForEnvironment(env, profileNames, profile));
        command.setEnvironment(env);
        command.setInstallFeatures(installFeatures);
        command.setInstallSources(installSources);
        command.setProfileProperties(profileProperties);
        getLog().info("Installing product " + product.getId() + " for environment " + env + " to "
                + destination.getAbsolutePath() + " using " + command.getProfileProperties());
        try {
            command.execute();
        } catch (DirectorCommandException e) {
            IStatus status = StatusTool.findStatus(e);
            if (status != null) {
                String logMessage = StatusTool.toLogMessage(status);
                getLog().error(logMessage);
                throw new MojoFailureException("Installation of product " + product.getId() + " for environment " + env
                        + " failed: " + logMessage, e);
            }
            throw new MojoFailureException(
                    "Installation of product " + product.getId() + " for environment " + env + " failed", e);
        }
    }

    private DirectorRuntime getDirectorRuntime() throws MojoFailureException, MojoExecutionException {
        return switch (directorRuntime) {
        case internal -> director;
        case standalone -> standaloneDirectorFactory.createStandaloneDirector(getBuildDirectory().getChild("director"),
                getSession().getLocalRepository(), getForkedProcessTimeoutInSeconds());
        default -> throw new MojoFailureException(
                "Unsupported value for attribute 'directorRuntime': \"" + directorRuntime + "\"");
        };
    }

    private RepositoryReferences getSourceRepositories() throws MojoExecutionException, MojoFailureException {
        return switch (source) {
        case targetPlatform -> getTargetPlatformRepositories();
        case repository -> getBuildOutputRepository();
        default -> throw new MojoFailureException("Unsupported value for attribute 'source': \"" + source + "\"");
        };
    }

    private RepositoryReferences getBuildOutputRepository() {
        File buildOutputRepository = getBuildDirectory().getChild("repository");

        RepositoryReferences result = new RepositoryReferences();
        result.addMetadataRepository(buildOutputRepository);
        result.addArtifactRepository(buildOutputRepository);
        return result;
    }

    private RepositoryReferences getTargetPlatformRepositories() throws MojoExecutionException, MojoFailureException {
        int flags = RepositoryReferenceTool.REPOSITORIES_INCLUDE_CURRENT_MODULE;
        return repositoryReferenceTool.getVisibleRepositories(getProject(), getSession(), flags);
    }

    private Iterable<String> getProductEEProfiles() {
        if (EEProfiles != null && !EEProfiles.isEmpty()) {
            return Arrays.asList(EEProfiles.split(","));
        }
        return ExecutionEnvironmentUtils.getProfileNames(toolchainManager, getSession(), logger).stream()
                .filter(str -> str.startsWith("JavaSE-"))
                .filter(profile -> ExecutionEnvironmentUtils.getVersion(profile) >= 11).toList();
    }
}
